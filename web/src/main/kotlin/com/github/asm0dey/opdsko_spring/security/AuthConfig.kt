package com.github.asm0dey.opdsko_spring.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class that enables the AuthProperties.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties::class)
class AuthConfig