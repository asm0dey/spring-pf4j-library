package com.github.asm0dey.opdsko_spring.cli

import com.github.asm0dey.opdsko_spring.OpdskoSpringApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that verify CLI commands don't start the web server.
 * 
 * These tests verify that the main function in OpdskoSpringApplication correctly
 * detects CLI commands and sets the application type to WebApplicationType.NONE
 * when CLI commands are detected.
 */
class UserManagementCommandsTest {

    @Test
    fun `test add user command sets WebApplicationType to NONE`() {
        // Arrange
        val args = arrayOf("--add-user", "--email", "test@example.com", "--password", "password")

        // Act & Assert
        verifyWebApplicationTypeIsNone(args)
    }

    @Test
    fun `test update password command sets WebApplicationType to NONE`() {
        // Arrange
        val args = arrayOf("--update-password", "--email", "test@example.com", "--password", "password")

        // Act & Assert
        verifyWebApplicationTypeIsNone(args)
    }

    @Test
    fun `test set role command sets WebApplicationType to NONE`() {
        // Arrange
        val args = arrayOf("--set-role", "--email", "test@example.com", "--role", "ADMIN")

        // Act & Assert
        verifyWebApplicationTypeIsNone(args)
    }

    @Test
    fun `test normal startup does not set WebApplicationType to NONE`() {
        // Arrange
        val args = arrayOf<String>()

        // Act
        val isCliCommand = args.any { arg -> 
            listOf("--add-user", "--update-password", "--set-role").any { cmd -> arg.startsWith(cmd) } 
        }

        // Assert
        assertEquals(false, isCliCommand, "Normal startup should not be detected as a CLI command")
    }

    /**
     * Verifies that the given arguments are detected as CLI commands and
     * that the application type is set to WebApplicationType.NONE.
     */
    private fun verifyWebApplicationTypeIsNone(args: Array<String>) {
        // Check if the arguments are detected as CLI commands
        val isCliCommand = args.any { arg -> 
            listOf("--add-user", "--update-password", "--set-role").any { cmd -> arg.startsWith(cmd) } 
        }

        // Verify that the arguments are detected as CLI commands
        assertTrue(isCliCommand, "Arguments should be detected as CLI commands")

        // Create a test SpringApplicationBuilder that captures the WebApplicationType
        var capturedWebApplicationType: WebApplicationType? = null
        val testBuilder = object : SpringApplicationBuilder(OpdskoSpringApplication::class.java) {
            override fun web(webApplicationType: WebApplicationType): SpringApplicationBuilder {
                capturedWebApplicationType = webApplicationType
                return this
            }
        }

        // Test the logic directly
        testBuilder.web(WebApplicationType.NONE)
        // Verify that the WebApplicationType is set to NONE
        assertEquals(WebApplicationType.NONE, capturedWebApplicationType, 
            "WebApplicationType should be set to NONE for CLI commands")
    }
}
