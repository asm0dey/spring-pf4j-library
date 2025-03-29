package com.github.asm0dey.opdsko_spring

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
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

    suspend fun scan(request: ServerRequest): ServerResponse {
        logger.info("Starting book scanning process")
        var totalBooksProcessed = 0
        var totalSourcesProcessed = 0
        val scan = coroutineScope {
            async {
                try {
                    settings
                        .sources
                        .asSequence()
                        .flatMap {
                            totalSourcesProcessed++
                            logger.info("Scanning source: ${it.absolutePath}")
                            if (it.isDirectory) {
                                logger.info("Source is a directory, walking through files")
                                it.walkTopDown()
                                    .filter { it.isFile }
                                    .flatMap {
                                        logger.debug("Processing file: ${it.absolutePath}")
                                        bookService.obtainBooks(it.absolutePath)
                                    }
                            } else {
                                logger.info("Source is a file, processing directly")
                                bookService.obtainBooks(it.absolutePath)
                            }
                        }
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
                        .chunked(1000)
                        .forEach { processedBookChunk ->
                            logger.info("Saving chunk of ${processedBookChunk.size} books to MongoDB")
                            val savedBooks = bookRepo.save(processedBookChunk)
                            totalBooksProcessed += savedBooks.size

                            logger.info("Indexing ${savedBooks.size} books in Meilisearch")
                            val bookIndexItems = savedBooks.map { BookIndexItem(it.path, it.name) }
                            meilisearch.saveBooks(bookIndexItems)
                        }

                    logger.info("Book scanning completed. Processed $totalBooksProcessed books from $totalSourcesProcessed sources")
                } catch (e: Exception) {
                    logger.error("Error during book scanning: ${e.message}", e)
                    throw e
                }
            }
        }
        return ServerResponse.status(ACCEPTED).bodyValueAndAwait("Processing started").also { scan.await() }

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
        val cleanup = coroutineScope {
            async {
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
        }
        return ServerResponse.status(ACCEPTED).bodyValueAndAwait("Cleanup started").also { cleanup.await() }

    }
}
