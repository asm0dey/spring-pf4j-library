package com.github.asm0dey.opdsko_spring.security

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant

/**
 * MongoDB document representing a user with UserDetails implementation for Spring Security.
 *
 * @property id The MongoDB document ID
 * @property email The email address used as the username for authentication
 * @property password The encoded password
 * @property roles The user's roles (e.g., USER, ADMIN)
 * @property enabled Whether the user account is enabled
 * @property accountNonExpired Whether the user account is not expired
 * @property accountNonLocked Whether the user account is not locked
 * @property credentialsNonExpired Whether the user credentials are not expired
 * @property createdAt When the user was created
 * @property updatedAt When the user was last updated
 */
@Document(collection = "users")
data class MongoUserDetails(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val email: String,

    private var password: String,

    val roles: Set<String> = setOf("USER"),

    val enabled: Boolean = true,
    val accountNonExpired: Boolean = true,
    val accountNonLocked: Boolean = true,
    val credentialsNonExpired: Boolean = true,

    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) : UserDetails {

    override fun getUsername(): String = email


    override fun getPassword(): String = password

    fun updatePassword(newPassword: String) {
        password = newPassword
        updatedAt = Instant.now()
    }

    fun addRole(role: String) {
        roles.toMutableSet().apply {
            add(role)
        }
        updatedAt = Instant.now()
    }

    fun hasRole(role: String): Boolean = roles.contains(role)

    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority("ROLE_$it") }

    override fun isEnabled(): Boolean = enabled

    override fun isAccountNonExpired(): Boolean = accountNonExpired

    override fun isAccountNonLocked(): Boolean = accountNonLocked

    override fun isCredentialsNonExpired(): Boolean = credentialsNonExpired

}
