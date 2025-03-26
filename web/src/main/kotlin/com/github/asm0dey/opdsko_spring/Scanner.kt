package com.github.asm0dey.opdsko_spring

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import java.io.File

@Suppress("ArrayInDataClass")
@Configuration
@ConfigurationProperties(prefix = "scanner")
data class ScannerSettings(var sources: Array<File> = arrayOf())

@Component
class Scanner(
    val settings: ScannerSettings,
    private val bookRepo: BookRepo,
    private val bookService: BookService,
    private val meilisearch: Meilisearch,
    private val seaweedFSService: SeaweedFSService,
    private val bookMongoRepository: BookMongoRepository,
) {
    suspend fun scan(request: ServerRequest): ServerResponse {
        settings
            .sources
            .asSequence()
            .flatMap {
                if (it.isDirectory) {
                    it.walkTopDown()
                        .filter { it.isFile }
                        .flatMap { bookService.obtainBooks(it.absolutePath) }
                } else {
                    bookService.obtainBooks(it.absolutePath)
                }
            }
            .map { (commonBook, size) ->
                Book(
                    authors = commonBook.authors.map {
                        Author(
                            lastName = it.lastName ?: "",
                            firstName = it.firstName ?: "",
                            fullName = it.fullName(),
                            middleName = it.middleName,
                            nickname = it.nickname,
                        )
                    },
                    genres = commonBook.genres,
                    sequence = commonBook.sequenceName,
                    sequenceNumber = commonBook.sequenceNumber,
                    name = commonBook.title,
                    size = size,
                    path = commonBook.path,
                    hasCover = commonBook.cover != null
                )
            }
            .chunked(1000)
            .forEach { processedBookChunk ->
                val savedBooksFlow = bookRepo.save(processedBookChunk)
                val savedBooks = savedBooksFlow.toList()

                val bookIndexItems = savedBooks.map { BookIndexItem(it.path, it.name) }
                meilisearch.saveBooks(bookIndexItems)

            }
        return ServerResponse.ok().buildAndAwait()
    }

    /**
     * Cleans up books that are no longer available in the sources.
     * For each source, scans it and removes books if they are not available anymore.
     * Uses book handlers and delegate book handlers to determine if books still exist.
     *
     * @param request The server request
     * @return The server response
     */
    suspend fun cleanup(request: ServerRequest): ServerResponse {
        // Get all books from the database
        val allBooks = bookMongoRepository.findAll()

        val booksToRemove = allBooks
            .filter { !bookService.bookExists(it.path) }
            .toList()

        // Remove books that no longer exist
        if (booksToRemove.isNotEmpty()) {
            // Delete from MongoDB
            bookMongoRepository.deleteAll(booksToRemove)

            // Delete from Meilisearch
            meilisearch.deleteBooks(booksToRemove.map { it.id })

            // Delete book covers from SeaweedFS
            for (book in booksToRemove) {
                seaweedFSService.deleteBookCover(book.id)
            }
        }

        return ServerResponse.ok().buildAndAwait()
    }
}