package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import com.github.asm0dey.opdsko.common.FormatConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.DependsOn
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.math.sign
import com.github.asm0dey.opdsko.common.Book as CommonBook

// Data class to represent book cover data
data class BookCoverData(
    val inputStream: InputStream,
    val contentType: String
)

// Data class to represent book download data
data class BookDownloadData(
    val resource: Any, // Can be InputStreamResource or FileSystemResource
    val fileName: String,
    val contentType: String,
    val tempFile: File? = null // For temporary files that need to be deleted after use
)

// Enum to represent the result of an operation
enum class OperationResult {
    NOT_FOUND,
    BAD_REQUEST,
    SUCCESS
}

// Data class to represent the result of an operation with data
data class OperationResultWithData<T>(
    val result: OperationResult,
    val data: T? = null,
    val errorMessage: String? = null
)

@Service
@DependsOn("springPluginManager")
class BookService(
    val bookHandlers: List<BookHandler>,
    val delegates: List<DelegatingBookHandler>,
    val bookMongoRepository: BookMongoRepository,
    val formatConverters: List<FormatConverter>,
    val bookRepo: BookRepo,
    val seaweedFSService: SeaweedFSService
) {
    /**
     * Gets the cover content types for a list of books.
     * If available, retrieves the content type from SeaweedFS.
     * If not available in SeaweedFS, retrieves it from the book and saves it to SeaweedFS.
     *
     * @param books The list of books
     * @param seaweedFSService The SeaweedFS service to use
     * @return A map of book IDs to cover content types
     */
    fun imageTypes(books: List<Book>) = books
        .map { Pair(it.id, it) }
        .associate { (id, book) ->
            if (!book.hasCover) return@associate id!! to null
            var type = try {
                seaweedFSService.getBookCoverContentType(id!!)
            } catch (e: Exception) {
                null
            }
            if (type == null || type == "application/octet-stream") {
                val fb = obtainBook(book.path)
                if (fb?.cover != null && fb.coverContentType != null) {
                    seaweedFSService.saveBookCover(id!!, fb.cover!!, fb.coverContentType!!)
                    type = fb.coverContentType
                }
            }

            id!! to type
        }

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
    suspend fun shortDescriptions(bookWithInfos: List<Book>) =
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
                            bookMongoRepository.save(updatedBook)
                        }
                    }

                    val size = bookSize.humanReadable()
                    val seq = book.sequence
                    val seqNo = book.sequenceNumber

                    val fb = obtainBook(book.path)

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

    suspend fun getBookById(id: String): Book? {
        return bookMongoRepository.findById(id)
    }

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

    fun convertBook(path: String, targetFormat: String): File? {
        val sourceFormat = path.substringAfterLast('.')
        val converter = formatConverters.firstOrNull {
            it.sourceFormat.equals(sourceFormat, ignoreCase = true) &&
                    it.targetFormat.equals(targetFormat, ignoreCase = true)
        } ?: return null
        if (!converter.canConvert(sourceFormat)) return null
        return getBookData(path).use { converter.convert(it) }
    }

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

    suspend fun searchBookByName(searchTerm: String, page: Int): List<Book> =
        bookRepo.searchBookByName(searchTerm, page)

    suspend fun newBooks(page: Int): PagedBooks = bookRepo.newBooks(page)

    suspend fun findAuthorFirstLetters() = bookMongoRepository.findAuthorFirstLetters()

    suspend fun countExactLastNames(prefix: String) = bookMongoRepository.countExactLastNames(prefix)

    suspend fun findAuthorPrefixes(prefix: String, prefixLength: Int) =
        bookMongoRepository.findAuthorPrefixes(prefix, prefixLength)

    suspend fun findAuthorsByPrefix(prefix: String) = bookMongoRepository.findAuthorsByPrefix(prefix)

    suspend fun findSeriesByAuthorFullName(fullName: String) =
        bookMongoRepository.findSeriesByAuthorFullName(fullName)

    suspend fun findBooksBySeries(series: String, sort: Sort) = bookMongoRepository.findBooksBySeries(series, sort)

    suspend fun findBooksByAuthorWithoutSeriesFullName(fullName: String, sort: Sort) =
        bookMongoRepository.findBooksByAuthorWithoutSeriesFullName(fullName, sort)

    suspend fun findBooksByAuthorFullName(fullName: String, page: Int): PagedBooks {
        val pageable = PageRequest.of(page, 24, Sort.by(Sort.Direction.ASC, "name"))
        val books = bookMongoRepository.findBooksByAuthorFullName(fullName, pageable).toList()
        val total = bookMongoRepository.countByAuthorsFullName(fullName)
        return PagedBooks(books, total)
    }

    suspend fun saveBook(book: Book) = bookMongoRepository.save(book)

    fun generateFileName(book: Book, extension: String): String {
        val baseFileName = buildString {
            append(book.name.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

            if (book.sequence != null) {
                append(" [")
                append(book.sequence.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

                if (book.sequenceNumber != null) {
                    append(" #")
                    append(book.sequenceNumber)
                }

                append("]")
            }
        }

        return "$baseFileName.$extension"
    }

    fun getContentTypeForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "epub" -> "application/epub+zip"
            "fb2" -> "application/x-fictionbook+xml"
            "pdf" -> "application/pdf"
            "mobi" -> "application/x-mobipocket-ebook"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    /**
     * Gets a book cover preview image from SeaweedFS or retrieves it from the book on demand.
     * If the cover is not in SeaweedFS, it retrieves it from the book and caches it to SeaweedFS.
     * If the book doesn't have a cover, it returns NOT_FOUND.
     *
     * @param bookId The ID of the book
     * @param seaweedFSService The SeaweedFS service to use for retrieving and caching covers
     * @return OperationResultWithData containing the cover data if successful
     */
    suspend fun getBookCover(bookId: String): OperationResultWithData<BookCoverData> {
        val book = getBookById(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)

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
                    saveBook(updatedBook)
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
                    seaweedFSService.getBookCoverContentType(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
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
     * Prepares a book for download, optionally converting it to a different format.
     *
     * @param bookId The ID of the book to download
     * @param targetFormat Optional format to convert the book to
     * @return OperationResultWithData containing the download data if successful
     */
    suspend fun downloadBook(bookId: String, targetFormat: String? = null): OperationResultWithData<BookDownloadData> {
        val book = getBookById(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
        val originalExtension = book.path.substringAfterLast('.')

        if (targetFormat != null) {
            val convertedFile = convertBook(book.path, targetFormat)
                ?: return OperationResultWithData(
                    OperationResult.BAD_REQUEST,
                    errorMessage = "Conversion to $targetFormat not supported"
                )

            val fileName = generateFileName(book, targetFormat)
            val contentType = getContentTypeForExtension(targetFormat)

            return OperationResultWithData(
                OperationResult.SUCCESS,
                BookDownloadData(
                    resource = FileSystemResource(convertedFile),
                    fileName = fileName,
                    contentType = contentType,
                    tempFile = convertedFile
                )
            )
        }

        val fileName = generateFileName(book, originalExtension)
        val contentType = getContentTypeForExtension(originalExtension)
        val bookData = InputStreamResource(getBookData(book.path))

        return OperationResultWithData(
            OperationResult.SUCCESS,
            BookDownloadData(
                resource = bookData,
                fileName = fileName,
                contentType = contentType
            )
        )
    }


    /**
     * Gets a full-size book cover image.
     *
     * @param bookId The ID of the book
     * @param seaweedFSService The SeaweedFS service to use for retrieving covers
     * @return OperationResultWithData containing the full-size cover data if successful
     */
    suspend fun getFullBookCover(bookId: String): OperationResultWithData<BookCoverData> {
        val book = getBookById(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
        if (!book.hasCover) return OperationResultWithData(OperationResult.NOT_FOUND)

        val coverInputStream = seaweedFSService.getBookCover(bookId)

        if (coverInputStream == null) {
            val commonBook = obtainBook(book.path) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
            if (commonBook.cover == null || commonBook.coverContentType == null) {
                val updatedBook = book.copy(hasCover = false)
                saveBook(updatedBook)
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

}
