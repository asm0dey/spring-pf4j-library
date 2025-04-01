package com.github.asm0dey.opdsko_spring.cli

import com.github.asm0dey.opdsko_spring.security.MongoReactiveUserDetailsService
import com.github.asm0dey.opdsko_spring.security.MongoUserDetails
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.system.exitProcess

/**
 * Command-line interface for user management.
 * Handles commands for adding users, updating passwords, and setting roles.
 */
@Component
class UserManagementCommands(
    private val userDetailsService: MongoReactiveUserDetailsService,
    private val passwordEncoder: PasswordEncoder,
    private val applicationContext: ApplicationContext
) : ApplicationRunner {

    companion object {
        private const val ADD_USER_COMMAND = "add-user"
        private const val UPDATE_PASSWORD_COMMAND = "update-password"
        private const val SET_ROLE_COMMAND = "set-role"
        private const val EMAIL_PARAM = "email"
        private const val PASSWORD_PARAM = "password"
        private const val ROLE_PARAM = "role"
    }

    override fun run(args: ApplicationArguments) {
        val optionNames = args.optionNames

        when {
            optionNames.contains(ADD_USER_COMMAND) -> handleAddUser(args)
            optionNames.contains(UPDATE_PASSWORD_COMMAND) -> handleUpdatePassword(args)
            optionNames.contains(SET_ROLE_COMMAND) -> handleSetRole(args)
            // If no user management commands are present, continue a normal application startup
            else -> return
        }

        // Exit the application after handling the command
        if (!optionNames.contains("noexit"))
            exitApplication()
    }

    private fun handleAddUser(args: ApplicationArguments) {
        var email = getOptionValue(args, EMAIL_PARAM)
        var password = getOptionValue(args, PASSWORD_PARAM)

        if (email.isNullOrBlank()) {
            // If an email is not provided, prompt for it
            email = promptForEmail()
            if (email.isBlank()) {
                println("Error: Email is required")
                return
            }
        }

        if (password.isNullOrBlank()) {
            // If a password is not provided, prompt for it
            password = promptForPassword()
            if (password.isBlank()) {
                println("Error: Password is required")
                return
            }
        }

        try {
            // Check if a user already exists
            val exists = userDetailsService.emailExists(email)
                .block(Duration.ofSeconds(5))

            if (exists == true) {
                println("Error: User with email '$email' already exists")
                return
            }

            // Create a new user
            val encodedPassword = passwordEncoder.encode(password)
            val user = MongoUserDetails(
                email = email,
                password = encodedPassword,
                roles = setOf("USER")
            )

            try {
                userDetailsService.createUser(user)
                    .block(Duration.ofSeconds(5))
                println("User with email '$email' created successfully")
            } catch (e: Exception) {
                println("Error creating user: ${e.message}")
            }
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
        }
    }

    private fun handleUpdatePassword(args: ApplicationArguments) {
        var email = getOptionValue(args, EMAIL_PARAM)
        var password = getOptionValue(args, PASSWORD_PARAM)

        if (email.isNullOrBlank()) {
            // If an email is not provided, prompt for it
            email = promptForEmail()
            if (email.isBlank()) {
                println("Error: Email is required")
                return
            }
        }

        if (password.isNullOrBlank()) {
            // If a password is not provided, prompt for it
            password = promptForPassword()
        }

        try {
            // Update password by email
            val encodedPassword = passwordEncoder.encode(password)
            try {
                userDetailsService.updatePasswordByEmail(email, encodedPassword)
                    .block(Duration.ofSeconds(5))
                println("Password updated successfully for user with email '$email'")
            } catch (e: Exception) {
                println("Error updating password: ${e.message}")
            }
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
        }
    }

    private fun handleSetRole(args: ApplicationArguments) {
        var email = getOptionValue(args, EMAIL_PARAM)
        var role = getOptionValue(args, ROLE_PARAM)

        if (email.isNullOrBlank()) {
            // If an email is not provided, prompt for it
            email = promptForEmail()
            if (email.isBlank()) {
                println("Error: Email is required")
                return
            }
        }

        if (role.isNullOrBlank()) {
            // If a role is not provided, prompt for it
            role = promptForRole()
            if (role.isBlank()) {
                println("Error: Role is required")
                return
            }
        }

        try {
            // Update user role
            try {
                userDetailsService.addRole(email, role.uppercase())
                    .block(Duration.ofSeconds(5))
                println("Role '$role' added successfully to user with email '$email'")
            } catch (e: Exception) {
                println("Error setting role: ${e.message}")
            }
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
        }
    }

    private fun getOptionValue(args: ApplicationArguments, option: String): String? =
        if (args.containsOption(option)) args.getOptionValues(option).firstOrNull() else null

    private fun promptForEmail(): String {
        print("Enter email: ")
        return readlnOrNull() ?: ""
    }

    private fun promptForPassword(): String {
        print("Enter password: ")
        return readlnOrNull() ?: ""
    }

    private fun promptForRole(): String {
        print("Enter role (USER or ADMIN): ")
        return readlnOrNull() ?: ""
    }

    private fun exitApplication() {
        val exitCode = SpringApplication.exit(applicationContext, { 0 })
        exitProcess(exitCode)
    }
}
