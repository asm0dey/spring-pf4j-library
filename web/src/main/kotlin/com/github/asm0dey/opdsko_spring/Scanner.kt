package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko_spring.repo.BookMongoRepository
import com.github.asm0dey.opdsko_spring.repo.BookRepo
import com.github.asm0dey.opdsko_spring.repo.Meilisearch
import com.github.asm0dey.opdsko_spring.service.BookService
import com.github.asm0dey.opdsko_spring.service.SeaweedFSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
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
    private val logger = LoggerFactory.getLogger(Scanner::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun scan(request: ServerRequest): ServerResponse {
        logger.info("Starting book scanning process")
        var totalBooksProcessed = 0
        var totalSourcesProcessed = 0
        CoroutineScope(Dispatchers.IO).launch {
            try {
                settings
                    .sources
                    .asSequence()
                    .asFlow()
                    .flatMapConcat {
                        totalSourcesProcessed++
                        logger.info("Scanning source: ${it.absolutePath}")
                        if (it.isDirectory) {
                            logger.info("Source is a directory, walking through files")
                            it.walkTopDown()
                                .filter { it.isFile }
                                .map {
                                    logger.debug("Processing file: ${it.absolutePath}")
                                    bookService.obtainBooks(it.absolutePath)
                                }
                                .asFlow()
                        } else {
                            logger.info("Source is a file, processing directly")
                            flowOf(bookService.obtainBooks(it.absolutePath))
                        }
                    }
                    .flattenConcat()
                    .filterNot { (commonBook, _) -> bookMongoRepository.existsByPathEquals(commonBook.path) }
                    .map { (commonBook, size) ->
                        Book(
                            authors = commonBook.authors.map {
                                Author(
                                    lastName = it.lastName ?: "",
                                    firstName = it.firstName ?: "",
                                    fullName = it.computeFullName(),
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
                    .chunked(10000)
                    .collect { processedBookChunk ->
                        logger.info("Saving chunk of ${processedBookChunk.size} books to MongoDB")
                        val savedBooks = bookRepo.save(processedBookChunk)
                        totalBooksProcessed += savedBooks.size
                    }
                resyncMeilisearch(request)
                logger.info("Book scanning completed. Processed $totalBooksProcessed books from $totalSourcesProcessed sources")
            } catch (e: Exception) {
                logger.error("Error during book scanning: ${e.message}", e)
                throw e
            }
        }
        return ServerResponse.status(ACCEPTED).bodyValueAndAwait("Processing started")
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
        logger.info("Starting book cleanup process")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get all books from the database
                logger.info("Retrieving all books from MongoDB")
                val allBooks = bookMongoRepository.findAll()
                logger.info("Retrieved books from MongoDB, checking for books to remove")

                val booksToRemove = allBooks
                    .filter {
                        val exists = bookService.bookExists(it.path)
                        if (!exists) {
                            logger.debug("Book no longer exists at path: ${it.path}")
                        }
                        !exists
                    }
                    .toList()

                logger.info("Found ${booksToRemove.size} books to remove")

                // Remove books that no longer exist
                if (booksToRemove.isNotEmpty()) {
                    // Delete from MongoDB
                    logger.info("Deleting ${booksToRemove.size} books from MongoDB")
                    bookMongoRepository.deleteAll(booksToRemove)

                    // Delete from Meilisearch
                    logger.info("Deleting ${booksToRemove.size} books from Meilisearch index")
                    meilisearch.deleteBooks(booksToRemove.map { it.id!! })

                    // Delete book covers from SeaweedFS
                    logger.info("Deleting book covers from SeaweedFS")
                    for (book in booksToRemove) {
                        logger.debug("Deleting cover for book: ${book.id} - ${book.name}")
                        seaweedFSService.deleteBookCover(book.id!!)
                    }

                    logger.info("Cleanup completed. Removed ${booksToRemove.size} books")
                } else {
                    logger.info("No books to remove, all books are still available")
                }

            } catch (e: Exception) {
                logger.error("Error during book cleanup: ${e.message}", e)
                throw e
            }
        }
        return ServerResponse.status(ACCEPTED).bodyValueAndAwait("Cleanup started")

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun resyncMeilisearch(request: ServerRequest): ServerResponse {
        logger.info("Starting Meilisearch resync process")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                meilisearch.deleteAllBooks()
                bookMongoRepository.findAllBy()
                    .chunked(10000)
                    .collectIndexed { index, chunk ->
                        logger.info("Indexing chunk ${index + 1} (${chunk.size} books)")
                        meilisearch.saveBooks(chunk)
                    }
                logger.info("Meilisearch resync completed.")
            } catch (e: Exception) {
                logger.error("Error during Meilisearch resync: ${e.message}", e)
                throw e
            }
        }
        return ServerResponse.status(ACCEPTED).bodyValueAndAwait("Meilisearch resync started")
    }
}
