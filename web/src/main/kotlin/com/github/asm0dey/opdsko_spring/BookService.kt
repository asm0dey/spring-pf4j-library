package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.Book
import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import generated.jooq.tables.interfaces.IBook
import net.lingala.zip4j.ZipFile
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import java.io.File
import java.text.StringCharacterIterator
import kotlin.math.abs
import kotlin.math.sign

@Service
@DependsOn("pluginManager")
class BookService(val bookHandlers: List<BookHandler>, val delegates: List<DelegatingBookHandler>) {
    fun imageTypes(books: List<BookWithInfo>) = books
        .map { Triple(it.id, it.book.path!!, it.book.zipFile) }
        .associate { (id, path, zipFile) ->
            val fb = obtainBook(zipFile, path)
            val type = fb?.coverContentType
            id to type
        }

    fun obtainBook(zipFile: String?, path: String): Book? {
        val dataExtractor = {
            if (zipFile == null) File(path).inputStream() else {
                val zip = ZipFile(zipFile)
                zip.use {
                    it.getInputStream(zip.getFileHeader(path))
                }
            }
        }
        return bookHandlers
            .firstOrNull { it.supportsFile(path, dataExtractor) }
            ?.bookInfo(path, dataExtractor)
    }

    fun obtainBooks(absolutePath: String): Sequence<Book?> {
        val file = File(absolutePath)
        return delegates
            .firstOrNull { it.supportFile(file) }
            ?.bookProvider(file)
            ?.mapNotNull { (name, dataProvider) ->
                bookHandlers
                    .firstOrNull { it.supportsFile(name, dataProvider) }
                    ?.bookInfo(name, dataProvider)
            }
            ?: sequenceOf(
                bookHandlers
                    .firstOrNull { it.supportsFile(absolutePath) { file.inputStream() } }
                    ?.bookInfo(absolutePath) { file.inputStream() }
            )
    }

    private val IBook.size
        get() = if (zipFile == null) File(path!!).length().humanReadable() else {
            ZipFile(zipFile).getFileHeader(path).uncompressedSize.humanReadable()
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
        bookWithInfos.map { it.id to it.book }
            .associate { (id, book) ->
                val size = book.size
                val seq = book.sequence
                val seqNo = book.sequenceNumber

                val fb = obtainBook(book.zipFile, book.path!!)


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