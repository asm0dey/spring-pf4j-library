package com.github.asm0dey.opdsko_spring.meilisearch

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.InetSocketAddress

interface MeilisearchConnectionDetails : ConnectionDetails {
    fun address(): InetSocketAddress
    fun key(): String
}

class MeilisearchPropertiesConnectionDetails(val properties: MeilisearchProperties) : MeilisearchConnectionDetails {
    override fun address() = InetSocketAddress(
        properties.host,
        properties.port
    )

    override fun key(): String {
        return properties.apiKey
    }
}

@ConfigurationProperties(prefix = "meilisearch")
class MeilisearchProperties {
    var apiKey = ""
    var host = ""
    var port = 7700
}
