package com.github.asm0dey.opdsko_spring.security

import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Implementation of ReactiveUserDetailsService that uses MongoDB to store and retrieve user details.
 * This service is used by Spring Security for user authentication.
 */
@Service
class MongoReactiveUserDetailsService(private val userRepository: MongoUserRepository) : ReactiveUserDetailsService {

    /**
     * Finds a user by username (which is the email).
     *
     * @param username The email to search for
     * @return A Mono that emits the user if found, or an error if not found
     */
    override fun findByUsername(username: String): Mono<UserDetails> {
        return userRepository.findByEmail(username)
            .switchIfEmpty { Mono.error(UsernameNotFoundException("User not found: $username")) }
            .cast(UserDetails::class.java)
    }

    /**
     * Finds a user by email.
     *
     * @param email The email to search for
     * @return A Mono that emits the user if found, or an error if not found
     */
    fun findByEmail(email: String): Mono<MongoUserDetails> {
        return userRepository.findByEmail(email)
            .switchIfEmpty { Mono.error(UsernameNotFoundException("User not found with email: $email")) }
    }

    /**
     * Creates a new user.
     *
     * @param user The user to create
     * @return A Mono that emits the created user
     */
    open fun createUser(user: MongoUserDetails): Mono<MongoUserDetails> {
        return userRepository.save(user)
    }

    /**
     * Updates an existing user.
     *
     * @param user The user to update
     * @return A Mono that emits the updated user
     */
    fun updateUser(user: MongoUserDetails): Mono<MongoUserDetails> {
        return userRepository.save(user)
    }

    /**
     * Updates a user's password by email.
     *
     * @param email The email of the user to update
     * @param newPassword The new encoded password
     * @return A Mono that emits the updated user
     */
    fun updatePasswordByEmail(email: String, newPassword: String): Mono<MongoUserDetails> {
        return userRepository.findByEmail(email)
            .switchIfEmpty { Mono.error(UsernameNotFoundException("User not found with email: $email")) }
            .flatMap { user ->
                user.updatePassword(newPassword)
                userRepository.save(user)
            }
    }

    /**
     * Adds a role to a user.
     *
     * @param email The email of the user to update
     * @param role The role to add
     * @return A Mono that emits the updated user
     */
    fun addRole(email: String, role: String): Mono<MongoUserDetails> {
        return userRepository.findByEmail(email)
            .switchIfEmpty { Mono.error(UsernameNotFoundException("User not found: $email")) }
            .flatMap { user ->
                user.addRole(role)
                userRepository.save(user)
            }
    }

    /**
     * Checks if a user with the given email exists.
     *
     * @param email The email to check
     * @return A Mono that emits true if the user exists, false otherwise
     */
    open fun emailExists(email: String): Mono<Boolean> =
        userRepository.existsByEmail(email)

    fun findAdminUserWithEmail(email: String): Flux<MongoUserDetails> {
        return userRepository.findByRolesContainingAndEmailIsNot("ADMIN", email)
    }
}
