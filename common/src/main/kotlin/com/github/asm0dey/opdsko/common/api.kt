package com.github.asm0dey.opdsko.common

import org.pf4j.ExtensionPoint
import java.io.File
import java.io.InputStream

interface BookHandler : ExtensionPoint {
    fun supportsFile(fileName: String, data: () -> InputStream): Boolean
    fun bookInfo(fileName: String, dataProvider: () -> InputStream): Book
    val readFormats: List<String>
}

interface DelegatingBookHandler : ExtensionPoint {
    fun supportFile(file: File): Boolean
    fun obtainBooks(file: File, handlers: Collection<BookHandler>): Sequence<Book>
    fun supportsPath(path: String): Boolean
    fun obtainBook(path: String, handlers: Collection<BookHandler>): Book
}

/**
 * Extension point for format converters.
 * Implementations of this interface can convert files from one format to another.
 */
interface FormatConverter : ExtensionPoint {
    /**
     * The source format that this converter can handle.
     */
    val sourceFormat: String

    /**
     * The target format that this converter can produce.
     */
    val targetFormat: String

    /**
     * Checks if this converter can convert the given file.
     *
     * @param file The file to check.
     * @return True if this converter can convert the file, false otherwise.
     */
    fun canConvert(file: File): Boolean

    /**
     * Converts the given file to the target format.
     *
     * @param sourceFile The file to convert.
     * @param targetFile The file to write the converted content to. If null, a new file will be created
     *                   with the same name as the source file but with the target format extension.
     * @return The converted file.
     */
    fun convert(sourceFile: File, targetFile: File? = null): File
}

data class DelegatingBook(
    val book: Book,
) : Book {
    override val title: String
        get() = book.title
    override val cover: ByteArray?
        get() = book.cover
    override val coverContentType: String?
        get() = book.coverContentType
    override val annotation: String?
        get() = book.annotation
    override val authors: List<Author>
        get() = book.authors
    override val genres: List<String>
        get() = book.genres
    override val sequenceName: String?
        get() = book.sequenceName
    override val sequenceNumber: Int?
        get() = book.sequenceNumber
    override var path: String = book.path
        get() = field
        set(value) {
            field = value
        }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DelegatingBook

        if (sequenceNumber != other.sequenceNumber) return false
        if (title != other.title) return false
        if (cover != null) {
            if (other.cover == null) return false
            if (!cover.contentEquals(other.cover)) return false
        } else if (other.cover != null) return false
        if (coverContentType != other.coverContentType) return false
        if (annotation != other.annotation) return false
        if (authors != other.authors) return false
        if (genres != other.genres) return false
        if (sequenceName != other.sequenceName) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sequenceNumber ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + (cover?.contentHashCode() ?: 0)
        result = 31 * result + (coverContentType?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + authors.hashCode()
        result = 31 * result + genres.hashCode()
        result = 31 * result + (sequenceName?.hashCode() ?: 0)
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String {
        return "DelegatingBook(title='$title', coverContentType=$coverContentType, annotation=$annotation, authors=$authors, genres=$genres, sequenceName=$sequenceName, sequenceNumber=$sequenceNumber, path='$path')"
    }

}

