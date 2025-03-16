package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import java.io.File
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.math.sign
import com.github.asm0dey.opdsko.common.Book as CommonBook

@Service
@DependsOn("springPluginManager")
class BookService(val bookHandlers: List<BookHandler>, val delegates: List<DelegatingBookHandler>) {
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

}
