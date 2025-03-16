package com.github.asm0dey.opdsko_spring

import org.springframework.data.annotation.Id

/**
 * Result class for author letter aggregation queries
 */
data class AuthorLetterResult(@Id val id: String, val count: Int = 0)

/**
 * Result class for author aggregation queries
 */
data class AuthorResult(@Id val id: AuthorId) {
    data class AuthorId(val lastName: String, val firstName: String)
}

/**
 * Result class for series aggregation queries
 */
data class SeriesResult(@Id val id: String)

/**
 * Result class for count aggregation queries
 */
data class CountResult(val count: Int)
