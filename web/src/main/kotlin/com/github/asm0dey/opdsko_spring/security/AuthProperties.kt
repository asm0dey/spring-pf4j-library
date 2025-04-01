package com.github.asm0dey.opdsko_spring.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for authentication.
 *
 * @property enabled Whether authentication is enabled
 * @property allowedIps List of IP addresses or subnets (in CIDR notation, e.g., "192.168.1.0/24") that can bypass authentication
 * @property allowedEmails List of email addresses that are allowed to authenticate
 * @property applicationUrl The URL of the application, used for OAuth2 redirect
 */
@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    val enabled: Boolean = false,
    val allowedIps: List<String> = emptyList(),
    val allowedEmails: List<String> = emptyList(),
    val applicationUrl: String = "http://localhost:8080",
)
