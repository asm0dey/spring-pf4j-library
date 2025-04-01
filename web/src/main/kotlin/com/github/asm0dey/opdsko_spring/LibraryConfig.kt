package com.github.asm0dey.opdsko_spring

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class that enables the LibraryProperties.
 */
@Configuration
@EnableConfigurationProperties(LibraryProperties::class)
class LibraryConfig