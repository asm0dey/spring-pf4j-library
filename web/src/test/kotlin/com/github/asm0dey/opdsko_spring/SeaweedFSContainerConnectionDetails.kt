package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchConnectionDetails
import com.github.opdsko_spring.seaweedfs.SeaweedFSConnectionDetails
import com.meilisearch.sdk.Client
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource
import org.testcontainers.containers.Container
import java.net.InetSocketAddress

class SeaweedFSContainerConnectionDetailsFactory :
    ContainerConnectionDetailsFactory<Container<*>, SeaweedFSConnectionDetails>(
        "seaweedfs",
        "seaweedfs.client.FilerClient2"
    ) {
    override fun getContainerConnectionDetails(source: ContainerConnectionSource<Container<*>>?): SeaweedFSConnectionDetails {
        return SeaweedFSContainerConnectionDetails(source)
    }

    private class SeaweedFSContainerConnectionDetails(source: ContainerConnectionSource<Container<*>>?) :
        ContainerConnectionDetails<Container<*>>(source), SeaweedFSConnectionDetails {
        override fun masterAddress(): InetSocketAddress =
            InetSocketAddress(container.host, container.getMappedPort(9333))

        override fun filerAddress(): InetSocketAddress =
            InetSocketAddress(container.host, container.getMappedPort(8888))

        override fun filerGrpcAddress(): InetSocketAddress =
            InetSocketAddress(container.host, container.getMappedPort(18888))
    }
}

class MeilisearchContainerConnectionDetailsFactory :
    ContainerConnectionDetailsFactory<Container<*>, MeilisearchConnectionDetails>(
        "meilisearch",
        Client::class.java.name
    ) {

    override fun getContainerConnectionDetails(source: ContainerConnectionSource<Container<*>>?): MeilisearchConnectionDetails {
        return MeilisearchContainerConnectionDetails(source)
    }

    private class MeilisearchContainerConnectionDetails(source: ContainerConnectionSource<Container<*>>?) :
        ContainerConnectionDetails<Container<*>>(source), MeilisearchConnectionDetails {

        override fun address(): String? = container.host + ":" + container.getMappedPort(7700)

        override fun key(): String? =
            container.envMap["MEILI_MASTER_KEY"]
    }
}