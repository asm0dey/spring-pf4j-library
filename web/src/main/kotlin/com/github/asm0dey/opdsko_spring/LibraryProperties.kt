package com.github.asm0dey.opdsko_spring

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the library application.
 *
 * @property title The title of the library application displayed in the UI
 */
@ConfigurationProperties(prefix = "library")
data class LibraryProperties(
    val title: String = "Asm0dey's library"
)