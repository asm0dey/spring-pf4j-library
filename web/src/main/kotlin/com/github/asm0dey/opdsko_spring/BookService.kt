package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import com.github.asm0dey.opdsko.common.FormatConverter
import org.springframework.context.annotation.DependsOn
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.text.StringCharacterIterator
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.sign
import com.github.asm0dey.opdsko.common.Book as CommonBook

@Service
@DependsOn("springPluginManager")
class BookService(
    val bookHandlers: List<BookHandler>,
    val delegates: List<DelegatingBookHandler>,
    val bookMongoRepository: BookMongoRepository,
    val formatConverters: List<FormatConverter>,
    val bookRepo: BookRepo
) {
    fun imageTypes(books: List<BookWithInfo>) = books
        .map { Pair(it.id, it.path) }
        .associate { (id, path) ->
            val fb = obtainBook(path)
            val type = fb?.coverContentType
            id to type
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


    fun obtainBooks(absolutePath: String): Sequence<CommonBook?> {
        val file = File(absolutePath)
        return delegates
            .firstOrNull { it.supportFile(file) }
            ?.obtainBooks(file, bookHandlers)
            ?: sequenceOf(
                bookHandlers
                    .firstOrNull { it.supportsFile(absolutePath) { file.inputStream() } }
                    ?.bookInfo(absolutePath) { file.inputStream() }
            )
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

    fun shortDescriptions(bookWithInfos: List<BookWithInfo>) =
        bookWithInfos.map { it.id to it }
            .associate { (id, book) ->
                val size = book.size.humanReadable()
                val seq = book.sequence
                val seqNo = book.sequenceNumber

                val fb = obtainBook(book.path)

                val descr = fb?.annotation ?: ""
                val text = buildString {
                    append("Size: $size.\n ")
                    seq?.let { append("Series: $it") }
                    seqNo?.let { append("#${it.toString().padStart(3, '0')}") }
                    seq?.let { append(".\n ") }
                    append(descr)
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

    fun convertBook(path: String, targetFormat: String): InputStream? {
        // Get the source format from the path
        val sourceFormat = path.substringAfterLast('.')

        // Find a suitable converter
        val converter = formatConverters.firstOrNull {
            it.sourceFormat.equals(sourceFormat, ignoreCase = true) &&
                    it.targetFormat.equals(targetFormat, ignoreCase = true)
        } ?: return null

        // Check if the converter can handle this format
        if (!converter.canConvert(sourceFormat)) {
            return null
        }

        // Get the book data as a stream and convert it
        return getBookData(path).use { converter.convert(it) }
    }

    /**
     * Checks if a book exists at the given path using book handlers.
     *
     * @param path The path to check
     * @return true if a book exists at the path, false otherwise
     */
    fun bookExists(path: String): Boolean {
        // First check if the file exists
        val file = File(path)
        if (!file.exists()) {
            return false
        }

        // Then try to obtain the book using handlers
        return try {
            // Check if any delegate handler supports this path
            val delegateSupports = delegates.any { it.supportsPath(path) }

            // If a delegate supports it, try to obtain the book
            if (delegateSupports) {
                delegates.firstOrNull { it.supportsPath(path) }
                    ?.obtainBook(path, bookHandlers) != null
            } else {
                // Otherwise, check if any book handler supports this file
                val dataExtractor = { file.inputStream() }
                bookHandlers.firstOrNull { it.supportsFile(path, dataExtractor) }
                    ?.bookInfo(path, dataExtractor) != null
            }
        } catch (e: Exception) {
            // If an exception occurs, the book doesn't exist or is corrupted
            false
        }
    }

    /**
     * Searches for books by text.
     *
     * @param searchTerm The search term
     * @param page The page number (0-based)
     * @return A list of books matching the search term
     */
    suspend fun searchBookByText(searchTerm: String, page: Int): List<BookWithInfo> {
        return bookRepo.searchBookByText(searchTerm, page)
    }

    /**
     * Gets new books.
     *
     * @param page The page number (0-based)
     * @return A list of new books
     */
    suspend fun newBooks(page: Int): List<BookWithInfo> {
        return bookRepo.newBooks(page)
    }

    /**
     * Finds author first letters.
     *
     * @return A flow of author letter results
     */
    suspend fun findAuthorFirstLetters() = bookMongoRepository.findAuthorFirstLetters()

    /**
     * Counts exact last names.
     *
     * @param prefix The prefix to match
     * @return A flow of count results
     */
    suspend fun countExactLastNames(prefix: String) = bookMongoRepository.countExactLastNames(prefix)

    /**
     * Finds author prefixes.
     *
     * @param prefix The prefix to match
     * @param prefixLength The length of the prefix
     * @return A flow of author letter results
     */
    suspend fun findAuthorPrefixes(prefix: String, prefixLength: Int) = bookMongoRepository.findAuthorPrefixes(prefix, prefixLength)

    /**
     * Finds authors by prefix.
     *
     * @param prefix The prefix to match
     * @return A flow of author results
     */
    suspend fun findAuthorsByPrefix(prefix: String) = bookMongoRepository.findAuthorsByPrefix(prefix)

    /**
     * Finds series by author.
     *
     * @param lastName The author's last name
     * @param firstName The author's first name
     * @return A flow of series results
     */
    suspend fun findSeriesByAuthor(lastName: String, firstName: String) = bookMongoRepository.findSeriesByAuthor(lastName, firstName)

    /**
     * Finds books by series.
     *
     * @param series The series name
     * @param sort The sort order
     * @return A flow of books
     */
    suspend fun findBooksBySeries(series: String, sort: Sort) = bookMongoRepository.findBooksBySeries(series, sort)

    /**
     * Finds books by author without series.
     *
     * @param lastName The author's last name
     * @param firstName The author's first name
     * @param sort The sort order
     * @return A flow of books
     */
    suspend fun findBooksByAuthorWithoutSeries(lastName: String, firstName: String, sort: Sort) = bookMongoRepository.findBooksByAuthorWithoutSeries(lastName, firstName, sort)

    /**
     * Finds books by author.
     *
     * @param lastName The author's last name
     * @param firstName The author's first name
     * @param sort The sort order
     * @return A flow of books
     */
    suspend fun findBooksByAuthor(lastName: String, firstName: String, sort: Sort) = bookMongoRepository.findBooksByAuthor(lastName, firstName, sort)

    /**
     * Saves a book.
     *
     * @param book The book to save
     * @return The saved book
     */
    suspend fun saveBook(book: Book) = bookMongoRepository.save(book)
}
