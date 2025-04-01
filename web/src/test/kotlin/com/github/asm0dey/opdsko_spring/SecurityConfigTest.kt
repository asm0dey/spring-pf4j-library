package com.github.asm0dey.opdsko_spring

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTest {
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

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `test root endpoint should not require authentication`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)

        // The root endpoint should not return 401 Unauthorized or 302 Found
        assert(response.statusCode != HttpStatus.UNAUTHORIZED && response.statusCode != HttpStatus.FOUND) {
            "Expected any status code except 401 Unauthorized or 302 Found but got ${response.statusCode}"
        }
    }

    @Test
    fun `test api endpoint should require authentication`() {
        val response = restTemplate.getForEntity("http://localhost:$port/api", String::class.java)

        // If we get a 200 OK, check if the response body contains a login form
        if (response.statusCode == HttpStatus.OK) {
            val body = response.body ?: ""
            assert(body.contains("login") || body.contains("Login")) {
                "Expected response body to contain a login form but got: $body"
            }
        } else {
            // Otherwise, check if we got a redirect to the login page
            assert(response.statusCode == HttpStatus.FOUND) {
                "Expected 302 Found (redirect to login page) but got ${response.statusCode}"
            }

            val location = response.headers.location
            assert(location != null && location.path.contains("/login")) {
                "Expected redirect to login page but got ${location?.path}"
            }
        }
    }

    @Test
    fun `test simple endpoint should require authentication`() {
        val response = restTemplate.getForEntity("http://localhost:$port/simple", String::class.java)

        // If we get a 200 OK, check if the response body contains a login form
        if (response.statusCode == HttpStatus.OK) {
            val body = response.body ?: ""
            assert(body.contains("login") || body.contains("Login")) {
                "Expected response body to contain a login form but got: $body"
            }
        } else {
            // Otherwise, check if we got a redirect to the login page
            assert(response.statusCode == HttpStatus.FOUND) {
                "Expected 302 Found (redirect to login page) but got ${response.statusCode}"
            }

            val location = response.headers.location
            assert(location != null && location.path.contains("/login")) {
                "Expected redirect to login page but got ${location?.path}"
            }
        }
    }

    @Test
    fun `test opds endpoint should require basic authentication`() {
        val response = restTemplate.getForEntity("http://localhost:$port/opds", String::class.java)

        // The opds endpoint should return 401 Unauthorized
        assert(response.statusCode == HttpStatus.UNAUTHORIZED) {
            "Expected 401 Unauthorized but got ${response.statusCode}"
        }
    }
}
