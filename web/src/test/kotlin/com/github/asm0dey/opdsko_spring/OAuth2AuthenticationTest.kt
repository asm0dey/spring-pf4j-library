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
class OAuth2AuthenticationTest {
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
    fun `test login with Google link no longer returns 404 error`() {
        // First, get the login page
        val loginPageResponse = restTemplate.getForEntity("http://localhost:$port/login", String::class.java)

        // Verify that the login page is returned successfully
        assert(loginPageResponse.statusCode == HttpStatus.OK) {
            "Expected 200 OK for login page but got ${loginPageResponse.statusCode}"
        }

        // Verify that the login page contains the "Login with Google" link
        val loginPageBody = loginPageResponse.body ?: ""
        assert(loginPageBody.contains("Login with Google")) {
            "Expected login page to contain 'Login with Google' link but it doesn't"
        }

        // Now, simulate clicking the "Login with Google" link
        val googleAuthResponse = restTemplate.getForEntity(
            "http://localhost:$port/oauth2/authorization/google", 
            String::class.java
        )

        // Verify that the response is not a 404 Not Found
        println("[DEBUG_LOG] Google auth response status: ${googleAuthResponse.statusCode}")
        assert(googleAuthResponse.statusCode != HttpStatus.NOT_FOUND) {
            "Expected any status code except 404 Not Found for Google auth endpoint but got ${googleAuthResponse.statusCode}"
        }

        // The response should be either:
        // 1. A redirect to Google's authorization page (302 Found)
        // 2. A redirect to the login page with an error parameter (if Google client ID is not valid)
        // 3. A 200 OK response with some content (if the OAuth2 endpoint is handled but not redirecting)
        assert(googleAuthResponse.statusCode == HttpStatus.FOUND || googleAuthResponse.statusCode == HttpStatus.OK) {
            "Expected either 302 Found or 200 OK for Google auth endpoint but got ${googleAuthResponse.statusCode}"
        }

        // If it's a redirect, print the location header for debugging
        if (googleAuthResponse.statusCode == HttpStatus.FOUND) {
            println("[DEBUG_LOG] Google auth response location: ${googleAuthResponse.headers.location}")
        }

        // If it's a 200 OK, print the response body for debugging
        if (googleAuthResponse.statusCode == HttpStatus.OK) {
            println("[DEBUG_LOG] Google auth response body: ${googleAuthResponse.body}")
        }
    }
}
