package com.github.asm0dey.opdsko_spring.meilisearch

import org.springframework.boot.docker.compose.core.RunningService
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource
import java.net.InetSocketAddress
import com.meilisearch.sdk.Client as MeilisearchClient

class MeilisearchDockerComposeConnectionDetailsFactory :
    DockerComposeConnectionDetailsFactory<MeilisearchConnectionDetails>(
        "getmeili/meilisearch", MeilisearchClient::class.qualifiedName!!
    ) {

    override fun getDockerComposeConnectionDetails(source: DockerComposeConnectionSource): MeilisearchConnectionDetails {
        return MeilisearchDockerComposeConnectionDetails(source.runningService)
    }

    private class MeilisearchDockerComposeConnectionDetails(val runningService: RunningService) :
        DockerComposeConnectionDetails(runningService),
        MeilisearchConnectionDetails {
        override fun address() = InetSocketAddress(
            runningService.host() ?: "localhost",
            runningService.ports()[7700]
        )

        override fun key() = runningService.env()["MEILI_MASTER_KEY"]!!
    }
}
