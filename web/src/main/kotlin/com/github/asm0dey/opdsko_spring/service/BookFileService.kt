package com.github.asm0dey.opdsko_spring.service

import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import com.github.asm0dey.opdsko_spring.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.math.sign
import com.github.asm0dey.opdsko.common.Book as CommonBook

/**
 * Service responsible for file operations related to books.
 */
@Service
class BookFileService(
    private val bookHandlers: List<BookHandler>,
    private val delegates: List<DelegatingBookHandler>,
    private val seaweedFSService: SeaweedFSService,
    private val bookDataService: BookDataService
) {
    private val logger = LoggerFactory.getLogger(BookFileService::class.java)

    /**
     * Gets the cover content types for a list of books.
     * If available, retrieves the content type from SeaweedFS.
     * If not available in SeaweedFS, retrieves it from the book and saves it to SeaweedFS.
     *
     * @param books The list of books
     * @return A map of book IDs to cover content types
     */
    fun imageTypes(books: List<Book>): Map<String, String?> = books
        .map { Pair(it.id, it) }
        .associate { (id, book) ->
            if (!book.hasCover) return@associate id!! to null
            var type = try {
                seaweedFSService.getBookCoverContentType(id!!)
            } catch (e: Exception) {
                logger.error("Unable to get cover content type for book $id from Seaweed", e)
                null
            }
            if (type == null || type == "application/octet-stream") {
                val fb = try {
                    obtainBook(book.path)
                } catch (e: Exception) {
                    logger.error("Unable to get cover content type for book $id from file ${book.path}", e)
                    return@associate id!! to null
                }
                if (fb?.cover != null && fb.coverContentType != null) {
                    seaweedFSService.saveBookCover(id!!, fb.cover!!, fb.coverContentType!!)
                    type = fb.coverContentType
                }
            }

            id!! to type
        }

    /**
     * Obtains a book from a path.
     *
     * @param path The path to the book
     * @return The book, or null if not found
     */
    fun obtainBook(path: String): CommonBook? {
        val delegatedBook = delegates
            .firstOrNull { it.supportsPath(path) }
            ?.obtainBook(path, bookHandlers)
        return if (delegatedBook != null) delegatedBook else {
            val dataExtractor = { File(path).inputStream() }
            return bookHandlers
                .firstOrNull { it.supportsFile(path, dataExtractor) }
                ?.bookInfo(path, dataExtractor)
        }
    }

    /**
     * Obtains books from a path.
     *
     * @param absolutePath The absolute path to the books
     * @return A flow of books and their sizes
     */
    fun obtainBooks(absolutePath: String): Flow<Pair<CommonBook, Long>> {
        val file = File(absolutePath)
        return delegates
            .firstOrNull { it.supportFile(file) }
            ?.obtainBooks(file, bookHandlers)
            ?: sequenceOf(
                bookHandlers
                    .firstOrNull { it.supportsFile(absolutePath) { file.inputStream() } }
                    ?.bookInfo(absolutePath) { file.inputStream() }
            )
                .filterNotNull()
                .map { it to file.length() }
                .asFlow()
    }

    /**
     * Gets the real size of a book.
     *
     * @param path The path to the book
     * @return The size of the book in bytes
     */
    fun getRealBookSize(path: String): Long = delegates
        .firstOrNull { it.supportsPath(path) }
        ?.obtainBookSize(path)
        ?: File(path).length()

    /**
     * Gets short descriptions for a list of books.
     * If available, retrieves the description from SeaweedFS.
     * If not available in SeaweedFS, generates it from the book and saves it to SeaweedFS.
     *
     * @param bookWithInfos The list of books
     * @return A map of book IDs to short descriptions
     */
    suspend fun shortDescriptions(bookWithInfos: List<Book>): Map<String, String> =
        bookWithInfos.map { it.id to it }
            .associate { (id, book) ->
                // First try to get the description from SeaweedFS
                var text = seaweedFSService.getBookDescription(id!!)

                // If not available in SeaweedFS, generate it and save it
                if (text == null) {
                    // Check if the size is 0 and get the real size if needed
                    var bookSize = book.size
                    if (bookSize == 0L) {
                        bookSize = getRealBookSize(book.path)
                        if (bookSize > 0) {
                            // Update the MongoDB record with the real size
                            val updatedBook = book.copy(size = bookSize)
                            bookDataService.saveBook(updatedBook)
                        }
                    }

                    val size = bookSize.humanReadable()
                    val seq = book.sequence
                    val seqNo = book.sequenceNumber

                    val fb = try {
                        obtainBook(book.path)
                    } catch (e: Exception) {
                        logger.error("Unable to obtain book ${book.id} from path ${book.path}", e)
                        null
                    }

                    val descr = fb?.annotation ?: ""
                    text = buildString {
                        append("Size: $size.\n ")
                        seq?.let { append("Series: $it") }
                        seqNo?.let { append("#${it.toString().padStart(3, '0')}") }
                        seq?.let { append(".\n ") }
                        append(descr)
                    }

                    // Save the description to SeaweedFS for future use
                    seaweedFSService.saveBookDescription(id, text)
                }

                id to text
            }

    /**
     * Gets the data of a book.
     *
     * @param path The path to the book
     * @return The input stream of the book data
     */
    fun getBookData(path: String): InputStream {
        val delegatedBook = delegates
            .firstOrNull { it.supportsPath(path) }
        return if (delegatedBook != null) {
            delegatedBook.getData(path, bookHandlers)
        } else {
            val handler = bookHandlers
                .firstOrNull { it.supportsFile(path) { File(path).inputStream() } }
                ?: throw IllegalArgumentException("No handler found for path: $path")
            handler.getData(path)
        }
    }

    /**
     * Checks if a book exists.
     *
     * @param path The path to the book
     * @return True if the book exists, false otherwise
     */
    fun bookExists(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) return false

        return try {
            val delegateSupports = delegates.any { it.supportsPath(path) }
            if (delegateSupports) delegates.firstOrNull { it.supportsPath(path) }
                ?.obtainBook(path, bookHandlers) != null
            else {
                val dataExtractor = { file.inputStream() }
                bookHandlers.firstOrNull { it.supportsFile(path, dataExtractor) }
                    ?.bookInfo(path, dataExtractor) != null
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets a book cover preview image from SeaweedFS or retrieves it from the book on demand.
     * If the cover is not in SeaweedFS, it retrieves it from the book and caches it to SeaweedFS.
     * If the book doesn't have a cover, it returns NOT_FOUND.
     *
     * @param bookId The ID of the book
     * @return OperationResultWithData containing the cover data if successful
     */
    suspend fun getBookCover(bookId: String): OperationResultWithData<BookCoverData> {
        val book = bookDataService.getBookById(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)

        if (!book.hasCover) return OperationResultWithData(OperationResult.NOT_FOUND)

        var coverInputStream = seaweedFSService.getBookCoverPreview(bookId)
        val contentType: String

        if (coverInputStream == null) {
            // Try to get the original cover (the preview might not exist yet)
            coverInputStream = seaweedFSService.getBookCover(bookId)

            if (coverInputStream == null) {
                // If we can't find the cover in SeaweedFS, let's try to get it from the book itself
                // Cover not found in SeaweedFS, retrieve it from the book
                val commonBook = obtainBook(book.path) ?: return OperationResultWithData(OperationResult.NOT_FOUND)

                // Check if the book has a cover
                if (commonBook.cover == null || commonBook.coverContentType == null) {
                    // Update the book in the database to reflect that it doesn't have a cover
                    // This is a case where the hasCover flag was true but the book actually doesn't have a cover
                    val updatedBook = book.copy(hasCover = false)
                    bookDataService.saveBook(updatedBook)
                    return OperationResultWithData(OperationResult.NOT_FOUND)
                }

                // Cache the cover to SeaweedFS (this will also create and cache the preview)
                seaweedFSService.saveBookCover(bookId, commonBook.cover!!, commonBook.coverContentType!!)

                // Get the newly cached cover preview from SeaweedFS
                coverInputStream = seaweedFSService.getBookCoverPreview(bookId)
                    ?: return OperationResultWithData(OperationResult.NOT_FOUND)

                contentType = commonBook.coverContentType!!
            } else {
                // We found the original cover but not the preview, so we'll use the original
                // Get the content type of the cover image from SeaweedFS
                contentType =
                    seaweedFSService.getBookCoverContentType(bookId)
                        ?: return OperationResultWithData(OperationResult.NOT_FOUND)
            }
        } else {
            // Get the content type of the cover preview image from SeaweedFS
            contentType = seaweedFSService.getBookCoverPreviewContentType(bookId)
        }

        // Create the book cover data
        val bookCoverData = BookCoverData(coverInputStream, contentType)

        return OperationResultWithData(OperationResult.SUCCESS, bookCoverData)
    }

    /**
     * Gets a full-size book cover image.
     *
     * @param bookId The ID of the book
     * @return OperationResultWithData containing the full-size cover data if successful
     */
    suspend fun getFullBookCover(bookId: String): OperationResultWithData<BookCoverData> {
        val book = bookDataService.getBookById(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
        if (!book.hasCover) return OperationResultWithData(OperationResult.NOT_FOUND)

        val coverInputStream = seaweedFSService.getBookCover(bookId)

        if (coverInputStream == null) {
            val commonBook = obtainBook(book.path) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
            if (commonBook.cover == null || commonBook.coverContentType == null) {
                val updatedBook = book.copy(hasCover = false)
                bookDataService.saveBook(updatedBook)
                return OperationResultWithData(OperationResult.NOT_FOUND)
            }

            seaweedFSService.saveBookCover(bookId, commonBook.cover!!, commonBook.coverContentType!!)
            val newCoverInputStream = seaweedFSService.getBookCover(bookId)
                ?: return OperationResultWithData(OperationResult.NOT_FOUND)

            return OperationResultWithData(
                OperationResult.SUCCESS,
                BookCoverData(newCoverInputStream, commonBook.coverContentType!!)
            )
        } else {
            val contentType = seaweedFSService.getBookCoverContentType(bookId)
                ?: return OperationResultWithData(OperationResult.NOT_FOUND)

            return OperationResultWithData(
                OperationResult.SUCCESS,
                BookCoverData(coverInputStream, contentType)
            )
        }
    }

    /**
     * Converts a size in bytes to a human-readable string.
     *
     * @return The human-readable size string
     */
    fun Long.humanReadable(): String {
        val absB = if (this == Long.MIN_VALUE) Long.MAX_VALUE else abs(this)
        if (absB < 1024) {
            return "$this B"
        }
        var value = absB
        val ci = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= sign.toLong()
        return String.format("%.1f %ciB", value / 1024.0, ci.current())
    }
}