package com.github.asm0dey.opdsko_spring

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

private const val PG_USER = "username"
private const val PG_PASS = "password"

@SpringBootTest
@Testcontainers
class OpdskoSpringApplicationTests {

    @Test
    fun contextLoads() {
    }


    companion object {
        @Container
        @ServiceConnection("meilisearch")
        @JvmStatic
        val meilisearch = GenericContainer(DockerImageName.parse("getmeili/meilisearch:v0.27.0"))
            .withExposedPorts(7700)
            .withEnv("MEILI_MASTER_KEY", "masterKey")
            .waitingFor(Wait.forListeningPort())

        @Container
        @ServiceConnection("seaweedfs")
        @JvmStatic
        val seaweedfs = GenericContainer(DockerImageName.parse("chrislusf/seaweedfs"))
            .withExposedPorts(9333, 8888, 18888)
            .withCommand("server -master -filer -ip.bind=0.0.0.0")
            .waitingFor(Wait.forListeningPorts(9333, 8888, 18888))

        @Container
        @ServiceConnection("mongodb")
        @JvmStatic
        val mongodb = MongoDBContainer(DockerImageName.parse("mongo:latest"))

    }
}
