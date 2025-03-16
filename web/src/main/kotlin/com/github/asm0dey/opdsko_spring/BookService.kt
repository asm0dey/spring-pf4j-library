package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import com.github.asm0dey.opdsko.common.FormatConverter
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.math.sign
import com.github.asm0dey.opdsko.common.Book as CommonBook

@Service
@DependsOn("springPluginManager")
class BookService(
    val bookHandlers: List<BookHandler>, 
    val delegates: List<DelegatingBookHandler>,
    val bookMongoRepository: BookMongoRepository,
    val formatConverters: List<FormatConverter>
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
}
