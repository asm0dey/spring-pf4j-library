package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.Book
import com.github.asm0dey.opdsko.common.BookHandler
import com.github.asm0dey.opdsko.common.DelegatingBook
import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import net.lingala.zip4j.ZipFile
import org.pf4j.Extension
import java.io.File
import java.io.InputStream

private const val ZIP_PREFIX = "zip::"

private const val ZIP_DELIMITER = ".zip#"

private const val ZIP_EXTENSION = ".zip"

@Extension(points = [DelegatingBookHandler::class])
class ZipBookHandler : DelegatingBookHandler {

    override fun supportFile(file: File) = file.name.endsWith(ZIP_EXTENSION)
    override fun obtainBooks(file: File, handlers: Collection<BookHandler>): Sequence<Pair<Book, Long>> {
        return ZipFile(file).use { zip ->
            sequence {
                for (fileHeader in zip.fileHeaders) {
                    val book = handlers
                        .firstOrNull { it.supportsFile(fileHeader.fileName) { zip.getInputStream(fileHeader) } }
                        ?.bookInfo(fileHeader.fileName) { zip.getInputStream(fileHeader) } ?: continue
                    yield(DelegatingBook(book).also {
                        it.path = "$ZIP_PREFIX${file.absolutePath}#${book.path}"
                    } to fileHeader.uncompressedSize)
                }
            }
        }
    }

    override fun supportsPath(path: String) = path.startsWith(ZIP_PREFIX) && path.contains(ZIP_DELIMITER)
    override fun obtainBook(path: String, handlers: Collection<BookHandler>): Book {
        val zipPath = path.substringAfter(ZIP_PREFIX).substringBefore(ZIP_DELIMITER) + ZIP_EXTENSION
        val bookPath = path.substringAfter(ZIP_DELIMITER)
        return ZipFile(File(zipPath)).use { zip ->
            val handler = handlers
                .firstOrNull { it.supportsFile(bookPath) { zip.getInputStream(zip.getFileHeader(bookPath)) } }
                ?: error("Handler supporting $path not found")
            val book = handler.bookInfo(bookPath) { zip.getInputStream(zip.getFileHeader(bookPath)) }
            DelegatingBook(book).also { it.path = path }
        }
    }

    override fun getData(path: String, handlers: Collection<BookHandler>): InputStream {
        val zipPath = path.substringAfter(ZIP_PREFIX).substringBefore(ZIP_DELIMITER) + ZIP_EXTENSION
        val bookPath = path.substringAfter(ZIP_DELIMITER)
        val zip = ZipFile(File(zipPath))
        return zip.getInputStream(zip.getFileHeader(bookPath))
    }
}
