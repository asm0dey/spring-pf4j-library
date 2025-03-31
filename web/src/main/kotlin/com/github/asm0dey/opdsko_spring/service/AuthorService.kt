package com.github.asm0dey.opdsko_spring.service

import com.github.asm0dey.opdsko_spring.repo.BookMongoRepository
import org.springframework.stereotype.Service

/**
 * Service responsible for author-related operations.
 */
@Service
class AuthorService(
    private val bookMongoRepository: BookMongoRepository
) {
    /**
     * Finds the first letters of author names.
     *
     * @return A flow of first letters
     */
    suspend fun findAuthorFirstLetters() = bookMongoRepository.findAuthorFirstLetters()

    /**
     * Counts the exact last names with a given prefix.
     *
     * @param prefix The prefix
     * @return A flow of counts
     */
    suspend fun countExactLastNames(prefix: String) = bookMongoRepository.countExactLastNames(prefix)

    /**
     * Finds author prefixes with a given prefix and length.
     *
     * @param prefix The prefix
     * @param prefixLength The length of the prefix
     * @return A flow of prefixes
     */
    suspend fun findAuthorPrefixes(prefix: String, prefixLength: Int) =
        bookMongoRepository.findAuthorPrefixes(prefix, prefixLength)

    /**
     * Finds authors by prefix.
     *
     * @param prefix The prefix
     * @return A flow of authors
     */
    suspend fun findAuthorsByPrefix(prefix: String) = bookMongoRepository.findAuthorsByPrefix(prefix)

    /**
     * Finds series by author full name.
     *
     * @param fullName The author's full name
     * @return A flow of series
     */
    suspend fun findSeriesByAuthorFullName(fullName: String) =
        bookMongoRepository.findSeriesByAuthorFullName(fullName)
}