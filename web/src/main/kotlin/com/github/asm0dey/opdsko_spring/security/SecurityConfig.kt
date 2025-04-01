package com.github.asm0dey.opdsko_spring.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException

/**
 * Security configuration for the application.
 * Configures Google OAuth2 authentication, form-based authentication, basic authentication, and IP-based authentication bypass.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val authProperties: AuthProperties,
    private val mongoUserDetailsService: MongoReactiveUserDetailsService
) {

    /**
     * Provides the password encoder for the application.
     * Uses Argon2 for secure password hashing.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    /**
     * Configures the security filter chain for the root endpoint.
     * This endpoint redirects to /api and should use form-based authentication.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    fun rootSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        if (!authProperties.enabled) {
            return http {
                securityMatcher(PathPatternParserServerWebExchangeMatcher("/"))
                csrf { disable() }
                authorizeExchange {
                    authorize(anyExchange, permitAll)
                }
            }
        }

        return http {
            securityMatcher(PathPatternParserServerWebExchangeMatcher("/"))
            csrf { disable() }
            authorizeExchange { authorize(anyExchange, permitAll) }
        }
    }

    /**
     * Configures the security filter chain for the admin endpoints (/scan, /cleanup, /resync).
     * These endpoints are protected and require the ADMIN role.
     */
    @Bean
    @org.springframework.core.annotation.Order(5)
    fun adminEndpointsSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http {
            securityMatcher(pathMatchers("/scan", "/cleanup", "/resync"))
            csrf { disable() }
            if (!authProperties.enabled) {
                authorizeExchange {
                    authorize(anyExchange, permitAll)
                }
            } else {
                authorizeExchange {
                    authorize(anyExchange, hasRole("ADMIN"))
                }
                formLogin {
                    loginPage = "/login"
                }
                oauth2Login {
                    authenticationFailureHandler = authenticationFailureHandler()
                }
            }
        }

    }

    /**
     * Configures the security filter chain for static resources and IP-based authentication bypass.
     * This filter chain allows access to static resources and requests from allowed IPs.
     */
    @Bean
    @org.springframework.core.annotation.Order(6)
    fun staticResourcesSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        if (!authProperties.enabled) {
            return http {
                securityMatcher(PathPatternParserServerWebExchangeMatcher("/**"))
                csrf { disable() }
                authorizeExchange {
                    authorize(anyExchange, permitAll)
                }
            }
        }
        return http {
            securityMatcher(PathPatternParserServerWebExchangeMatcher("/**"))
            csrf { disable() }
            authorizeExchange {
                authorize(
                    pathMatchers(
                        "/favicon.ico",
                        "/static/**",
                        "/*.js",
                        "/*.css",
                        "/*.png",
                        "/*.svg",
                        "/",
                        "/oauth2/**",
                        "/logout",
                        "/webjars/**",
                    ), permitAll
                )
                authorize("/login", permitAll)
                authorize(IpAddressMatcher(authProperties.allowedIps), permitAll)
                authorize(anyExchange, authenticated)
            }
            formLogin {
                loginPage = "/login"
            }
            oauth2Login {
                authenticationFailureHandler = authenticationFailureHandler()
            }
        }
    }

    /**
     * Configures the security filter chain for the /opds endpoint.
     * This endpoint is protected by basic authentication.
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    fun opdsSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        if (!authProperties.enabled) {
            return http {
                securityMatcher(PathPatternParserServerWebExchangeMatcher("/opds/**"))
                csrf { disable() }
                authorizeExchange {
                    authorize(anyExchange, permitAll)
                }
            }
        }
        return http {
            securityMatcher(PathPatternParserServerWebExchangeMatcher("/opds/**"))
            csrf { disable() }
            authorizeExchange {
                authorize(anyExchange, authenticated)
//                authorize(anyExchange, permitAll)
            }
            httpBasic {}
        }
    }

    /**
     * Configures the security filter chain for the /simple endpoint.
     * This endpoint is protected by form and Google OAuth2 authentication.
     */
    @Bean
    @org.springframework.core.annotation.Order(3)
    fun simpleSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        if (!authProperties.enabled) {
            return http {
                securityMatcher(PathPatternParserServerWebExchangeMatcher("/simple/**"))
                csrf { disable() }
                authorizeExchange {
                    authorize(anyExchange, permitAll)
                }
            }
        }

        // Configure logout success handler to redirect to home page
        return http {
            securityMatcher(PathPatternParserServerWebExchangeMatcher("/simple/**"))
            csrf { disable() }
            authorizeExchange { authorize(anyExchange, authenticated) }
            formLogin {
                loginPage = "/login"
                authenticationSuccessHandler = ServerAuthenticationSuccessHandler { exchange, _ ->
                    val redirectUri = getRedirectUri(exchange, "/simple")
                    exchange.exchange.response.headers.location = URI(redirectUri)
                    Mono.empty()
                }
            }
            logout {
                logoutUrl = "/logout"
                val serverLogoutSuccessHandler = RedirectServerLogoutSuccessHandler()
                logoutSuccessHandler = serverLogoutSuccessHandler
            }
            oauth2Login {
                authenticationFailureHandler = authenticationFailureHandler()
                authenticationSuccessHandler = ServerAuthenticationSuccessHandler { exchange, authentication ->
                    // Get the email from the authentication object
                    val email = getEmailFromAuthentication(authentication)

                    // Check if the email is in the allowed list
                    if (authProperties.allowedEmails.contains(email)) {
                        // Email is allowed, check if a user with this email already exists
                        mongoUserDetailsService.emailExists(email)
                            .flatMap { exists ->
                                if (exists) {
                                    // User exists, check if it's a Gmail user
                                    mongoUserDetailsService.findByEmail(email)
                                        .flatMap { user ->
                                            // If a user is not already an admin, check if there are any admin users with Gmail accounts
                                            if (!user.hasRole("ADMIN")) {
                                                // Check if there are any admin users with Gmail accounts
                                                mongoUserDetailsService.findAdminUserWithEmail(email)
                                                    .collectList()
                                                    .flatMap { adminUsers ->
                                                        if (adminUsers.isNotEmpty()) {
                                                            // There are admin users with Gmail accounts, grant admin privileges
                                                            user.addRole("ADMIN")
                                                            mongoUserDetailsService.updateUser(user)
                                                        } else {
                                                            Mono.just(user)
                                                        }
                                                    }
                                            } else {
                                                Mono.just(user)
                                            }
                                        }
                                } else {
                                    // User doesn't exist, create a new one
                                    // For OAuth users, we don't have a password, so generate a random one
                                    val randomPassword =
                                        passwordEncoder().encode(java.util.UUID.randomUUID().toString())
                                    val newUser = MongoUserDetails(
                                        email = email,
                                        password = randomPassword,
                                        roles = setOf("USER")
                                    )
                                    mongoUserDetailsService.createUser(newUser)
                                }
                            }
                            .flatMap {
                                // Redirect to the application
                                exchange.exchange.response.headers.location = URI(
                                    getRedirectUri(
                                        exchange,
                                        "/simple"
                                    )
                                )
                                Mono.empty<Void>()
                            }
                            .onErrorResume {

                                // In case of error, still redirect to the application
                                exchange.exchange.response.headers.location = URI(
                                    getRedirectUri(
                                        exchange,
                                        "/simple"
                                    )
                                )
                                Mono.empty()
                            }
                    } else {
                        // Email is not allowed, throw custom exception
                        // This will be caught by the authentication failure handler
                        throw UnauthorizedEmailException(email)
                    }
                }
            }
        }
    }

    private fun getRedirectUri(exchange: WebFilterExchange, base: String) =
        exchange.exchange.request.headers.getFirst("Referer") ?: "${authProperties.applicationUrl}$base"

    /**
     * Configures the security filter chain for the /api endpoint.
     * This endpoint is protected by form authentication.
     */
    @Bean
    @org.springframework.core.annotation.Order(4)
    fun apiSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        if (!authProperties.enabled) {
            return http {
                securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/**"))
                csrf { disable() }
                authorizeExchange {
                    authorize(anyExchange, permitAll)
                }
            }
        }

        // Configure logout success handler to redirect to home page
        return http {
            securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/**"))
            csrf { disable() }
            authorizeExchange { authorize(anyExchange, authenticated) }
            formLogin {
                loginPage = "/login"
                authenticationSuccessHandler = ServerAuthenticationSuccessHandler { exchange, _ ->
                    val redirectUri = getRedirectUri(exchange, "/api")
                    exchange.exchange.response.headers.location = URI(redirectUri)
                    Mono.empty()
                }
            }
            logout {
                logoutUrl = "/logout"
                logoutSuccessHandler = RedirectServerLogoutSuccessHandler()
            }
            oauth2Login {
                authenticationFailureHandler = authenticationFailureHandler()
                authenticationSuccessHandler = ServerAuthenticationSuccessHandler { exchange, authentication ->
                    // Get the email from the authentication object
                    val email = getEmailFromAuthentication(authentication)

                    // Check if the email is in the allowed list
                    if (authProperties.allowedEmails.contains(email)) {
                        // Email is allowed, check if a user with this email already exists
                        mongoUserDetailsService.emailExists(email)
                            .flatMap { exists ->
                                if (exists) {
                                    // User exists, check if it's a Gmail user
                                    mongoUserDetailsService.findByEmail(email)
                                        .flatMap { user ->
                                            // If a user is not already an admin, check if there are any admin users with Gmail accounts
                                            if (!user.hasRole("ADMIN")) {
                                                // Check if there are any admin users with Gmail accounts
                                                mongoUserDetailsService.findAdminUserWithEmail(email)
                                                    .collectList()
                                                    .flatMap { adminUsers ->
                                                        if (adminUsers.isNotEmpty()) {
                                                            // There are admin users with Gmail accounts, grant admin privileges
                                                            user.addRole("ADMIN")
                                                            mongoUserDetailsService.updateUser(user)
                                                        } else {
                                                            Mono.just(user)
                                                        }
                                                    }
                                            } else {
                                                Mono.just(user)
                                            }
                                        }
                                } else {
                                    // User doesn't exist, create a new one
                                    // For OAuth users, we don't have a password, so generate a random one
                                    val randomPassword =
                                        passwordEncoder().encode(java.util.UUID.randomUUID().toString())
                                    val newUser = MongoUserDetails(
                                        email = email,
                                        password = randomPassword,
                                        roles = setOf("USER")
                                    )
                                    mongoUserDetailsService.createUser(newUser)
                                }
                            }
                            .flatMap {
                                // Redirect to the application
                                exchange.exchange.response.headers.location = URI(
                                    getRedirectUri(
                                        exchange,
                                        "/api"
                                    )
                                )
                                Mono.empty<Void>()
                            }
                            .onErrorResume {

                                // In case of error, still redirect to the application
                                exchange.exchange.response.headers.location = URI(
                                    getRedirectUri(
                                        exchange,
                                        "/api"
                                    )
                                )
                                Mono.empty()
                            }
                    } else {
                        // Email is not allowed, throw custom exception
                        // This will be caught by the authentication failure handler
                        throw UnauthorizedEmailException(email)
                    }
                }
            }

        }
    }

    /**
     * Authentication failure handler that redirects to the login page.
     * Handles both standard authentication failures and unauthorized email errors.
     */
    @Bean
    fun authenticationFailureHandler(): ServerAuthenticationFailureHandler {
        return ServerAuthenticationFailureHandler { exchange, exception ->
            val errorMessage = "Authentication failure: ${exception.message}"
            println(errorMessage)

            // Redirect to login page with error message
            val redirectUri = "${authProperties.applicationUrl}/oauth2/authorization/google?error=authentication_error"
            exchange.exchange.response.headers.location = URI(redirectUri)
            Mono.empty()
        }
    }

    /**
     * Custom exception for unauthorized email addresses.
     */
    class UnauthorizedEmailException(email: String) :
        RuntimeException("Email address not authorized: $email")

    /**
     * Extracts the email address from the authentication object.
     * For Google OAuth2 authentication, the email is in the attributes map.
     *
     * @param authentication The authentication object
     * @return The email address or a default value if not found
     */
    private fun getEmailFromAuthentication(authentication: Authentication): String {
        if (authentication is OAuth2AuthenticationToken) {
            val oauth2User = authentication.principal as OAuth2User
            return oauth2User.attributes["email"] as? String ?: authentication.name
        }
        return authentication.name
    }

    /**
     * Matcher that checks if the request comes from an allowed IP address or subnet.
     * Supports both single IP addresses and CIDR notation for subnets (e.g., "192.168.1.0/24").
     */
    inner class IpAddressMatcher(private val allowedIps: List<String>) : ServerWebExchangeMatcher {
        override fun matches(exchange: ServerWebExchange): Mono<ServerWebExchangeMatcher.MatchResult> {
            val remoteAddress = exchange.request.remoteAddress?.address?.hostAddress
                ?: return ServerWebExchangeMatcher.MatchResult.notMatch()

            for (allowedIp in allowedIps) {
                if (isIpMatch(remoteAddress, allowedIp)) {
                    return ServerWebExchangeMatcher.MatchResult.match()
                }
            }

            return ServerWebExchangeMatcher.MatchResult.notMatch()
        }

        /**
         * Checks if the given IP address matches the allowed IP or falls within the allowed subnet.
         *
         * @param ipAddress The IP address to check
         * @param allowedIp The allowed IP address or subnet in CIDR notation (e.g., "192.168.1.0/24")
         * @return true if the IP address matches or is in the subnet, false otherwise
         */
        private fun isIpMatch(ipAddress: String, allowedIp: String): Boolean {
            // Check for exact match first (backward compatibility)
            if (ipAddress == allowedIp) {
                return true
            }

            // Check if the allowed IP is a subnet (contains '/')
            if (allowedIp.contains('/')) {
                return isIpInSubnet(ipAddress, allowedIp)
            }

            return false
        }

        /**
         * Checks if an IP address is within a subnet specified in CIDR notation.
         *
         * @param ipAddress The IP address to check
         * @param cidrSubnet The subnet in CIDR notation (e.g., "192.168.1.0/24")
         * @return true if the IP address is in the subnet, false otherwise
         */
        private fun isIpInSubnet(ipAddress: String, cidrSubnet: String): Boolean {
            try {
                val parts = cidrSubnet.split('/')
                if (parts.size != 2) {
                    return false
                }

                val subnetAddress = parts[0]
                val prefixLength = parts[1].toIntOrNull() ?: return false

                // Handle IPv4
                if (!subnetAddress.contains(':') && !ipAddress.contains(':')) {
                    val ip = InetAddress.getByName(ipAddress).address
                    val subnet = InetAddress.getByName(subnetAddress).address

                    // Create subnet mask from prefix length
                    val mask = ByteArray(4)
                    for (i in 0 until 4) {
                        mask[i] = if (i * 8 < prefixLength) {
                            ((0xFF shl (8 - minOf(8, prefixLength - i * 8))) and 0xFF).toByte()
                        } else {
                            0
                        }
                    }

                    // Check if IP is in subnet
                    for (i in 0 until 4) {
                        if ((ip[i].toInt() and mask[i].toInt()) != (subnet[i].toInt() and mask[i].toInt())) {
                            return false
                        }
                    }

                    return true
                }
                // Handle IPv6 (simplified implementation)
                else if (subnetAddress.contains(':') && ipAddress.contains(':')) {
                    val ip = InetAddress.getByName(ipAddress).address
                    val subnet = InetAddress.getByName(subnetAddress).address

                    // Create subnet mask from prefix length
                    val maskBytes = ByteArray(16)
                    for (i in 0 until 16) {
                        maskBytes[i] = if (i * 8 < prefixLength) {
                            ((0xFF shl (8 - minOf(8, prefixLength - i * 8))) and 0xFF).toByte()
                        } else {
                            0
                        }
                    }

                    // Check if IP is in subnet
                    for (i in 0 until 16) {
                        if ((ip[i].toInt() and maskBytes[i].toInt()) != (subnet[i].toInt() and maskBytes[i].toInt())) {
                            return false
                        }
                    }

                    return true
                }

                return false
            } catch (e: UnknownHostException) {
                return false
            } catch (e: Exception) {
                return false
            }
        }
    }
}
