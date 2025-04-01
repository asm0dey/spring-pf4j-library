package com.github.asm0dey.opdsko_spring.security

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Repository for managing MongoUserDetails entities in MongoDB.
 * Uses Spring Data's reactive MongoDB support.
 */
@Repository
interface MongoUserRepository : ReactiveMongoRepository<MongoUserDetails, String> {

    /**
     * Find a user by email.
     *
     * @param email The email to search for
     * @return A Mono that emits the user if found, or empty if not found
     */
    fun findByEmail(email: String): Mono<MongoUserDetails>

    /**
     * Check if a user with the given email exists.
     *
     * @param email The email to check
     * @return A Mono that emits true if the user exists, false otherwise
     */
    fun existsByEmail(email: String): Mono<Boolean>
    fun findByRolesContaining(role: String): Flux<MongoUserDetails>
    fun findByRolesContainingAndEmailIsNot(role: String, email: String): Flux<MongoUserDetails>
}
