package com.github.asm0dey.opdsko_spring.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException

/**
 * Security configuration for the application.
 * Configures Google OAuth2 authentication and IP-based authentication bypass.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(private val authProperties: AuthProperties) {

    /**
     * Configures the security filter chain.
     * If authentication is enabled, it configures Google OAuth2 authentication and IP-based authentication bypass.
     * If authentication is disabled, it permits all requests.
     */
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        if (!authProperties.enabled) {
            return http
                .csrf { it.disable() }
                .authorizeExchange { it.anyExchange().permitAll() }
                .build()
        }

        return http
            .csrf { it.disable() }
            .authorizeExchange {
                it.matchers(IpAddressMatcher(authProperties.allowedIps)).permitAll()
                it.anyExchange().authenticated()
            }
            .oauth2Login {
                it.authenticationFailureHandler(authenticationFailureHandler())
                // Set the base URL for OAuth2 redirects and check if email is allowed
                it.authenticationSuccessHandler { exchange, authentication ->
                    // Get the email from the authentication object
                    val email = getEmailFromAuthentication(authentication)

                    // Check if the email is in the allowed list
                    if (authProperties.allowedEmails.contains(email)) {
                        // Email is allowed, redirect to the application
                        val redirectUri = "${authProperties.applicationUrl}/"
                        exchange.exchange.response.headers.location = URI(redirectUri)
                        Mono.empty()
                    } else {
                        // Email is not allowed, throw custom exception
                        // This will be caught by the authentication failure handler
                        throw UnauthorizedEmailException(email)
                    }
                }
            }
            .build()
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
