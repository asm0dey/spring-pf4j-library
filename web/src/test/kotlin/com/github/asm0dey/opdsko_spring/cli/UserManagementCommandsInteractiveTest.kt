package com.github.asm0dey.opdsko_spring.cli

import com.github.asm0dey.opdsko_spring.security.MongoReactiveUserDetailsService
import com.github.asm0dey.opdsko_spring.security.MongoUserDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.context.ApplicationContext
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant

/**
 * Tests for the interactive behavior of UserManagementCommands.
 *
 * These tests verify that the CLI commands prompt for email and password
 * when they are not provided as command-line arguments.
 */
@ExtendWith(MockitoExtension::class)
class UserManagementCommandsInteractiveTest {

    @Mock
    private lateinit var userDetailsService: MongoReactiveUserDetailsService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var applicationContext: ApplicationContext

    private lateinit var userManagementCommands: UserManagementCommands

    private val originalSystemIn = System.`in`

    @BeforeEach
    fun setUp() {
        userManagementCommands = UserManagementCommands(
            userDetailsService,
            passwordEncoder,
            applicationContext
        )
    }

    /**
     * Test that the --add-user command prompts for email and password when they are not provided.
     */
    @Test
    fun `test add user command prompts for email and password when not provided`() {
        // Arrange
        val email = "test@example.com"
        val password = "password"
        val encodedPassword = "encodedPassword"
        val simulatedInput = "$email\n$password\n"

        // Mock the System.in to provide the simulated input
        val inputStream: InputStream = ByteArrayInputStream(simulatedInput.toByteArray())
        System.setIn(inputStream)
        val args = DefaultApplicationArguments("--add-user", "--noexit")

        // Mock the userDetailsService to return false for emailExists
        doReturn(Mono.just(false)).whenever(userDetailsService).emailExists(email)

        // Mock the passwordEncoder to return the encoded password
        doReturn(encodedPassword).whenever(passwordEncoder).encode(password)
        val now = Instant.now()
        // Mock the userDetailsService to return a user when createUser is called
        val user = MongoUserDetails(email = email, password = encodedPassword, createdAt = now, updatedAt = now)
        doReturn(Mono.just(user)).whenever(userDetailsService).createUser(any())

        try {
            // Act
            userManagementCommands.run(args)

            // Assert
            verify(userDetailsService).emailExists(email)
            verify(passwordEncoder).encode(password)
            verify(userDetailsService).createUser(any())
        } finally {
            // Restore the original System.in
            System.setIn(originalSystemIn)
        }
    }

    /**
     * Test that the --update-password command prompts for email and password when they are not provided.
     */
    @Test
    fun `test update password command prompts for email and password when not provided`() {
        // Arrange
        val email = "test@example.com"
        val password = "password"
        val encodedPassword = "encodedPassword"
        val simulatedInput = "$email\n$password\n"

        // Mock the System.in to provide the simulated input
        val inputStream: InputStream = ByteArrayInputStream(simulatedInput.toByteArray())
        System.setIn(inputStream)
        val args = DefaultApplicationArguments("--update-password", "--noexit")

        // Mock the passwordEncoder to return the encoded password
        doReturn(encodedPassword).whenever(passwordEncoder).encode(password)

        // Mock the userDetailsService to return a user when updatePasswordByEmail is called
        val user = MongoUserDetails(email = email, password = encodedPassword)
        doReturn(Mono.just(user)).whenever(userDetailsService).updatePasswordByEmail(email, encodedPassword)

        try {
            // Act
            userManagementCommands.run(args)

            // Assert
            verify(passwordEncoder).encode(password)
            verify(userDetailsService).updatePasswordByEmail(email, encodedPassword)
        } finally {
            // Restore the original System.in
            System.setIn(originalSystemIn)
        }
    }

    /**
     * Test that the --set-role command prompts for email and role when they are not provided.
     */
    @Test
    fun `test set role command prompts for email and role when not provided`() {
        // Arrange
        val email = "test@example.com"
        val role = "ADMIN"
        val simulatedInput = "$email\n$role\n"

        // Mock the System.in to provide the simulated input
        val inputStream: InputStream = ByteArrayInputStream(simulatedInput.toByteArray())
        System.setIn(inputStream)
        val args = DefaultApplicationArguments("--set-role", "--email=$email", "--role=$role", "--noexit")

        // Mock the userDetailsService to return a user when addRole is called
        val user = MongoUserDetails(email = email, password = "password")
        doReturn(Mono.just(user)).whenever(userDetailsService).addRole(email, role.uppercase())
        // Act
        userManagementCommands.run(args)

        // Assert
        verify(userDetailsService).addRole(email, role.uppercase())
    }
}
