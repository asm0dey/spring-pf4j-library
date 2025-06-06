package com.github.asm0dey.opdsko.common

import kotlinx.coroutines.flow.Flow
import org.pf4j.ExtensionPoint
import java.io.File
import java.io.InputStream

interface BookHandler : ExtensionPoint {
    fun supportsFile(fileName: String, data: () -> InputStream): Boolean
    fun bookInfo(fileName: String, dataProvider: () -> InputStream): Book
    fun getData(path: String): InputStream
    val readFormats: List<String>
}

interface DelegatingBookHandler : ExtensionPoint {
    fun supportFile(file: File): Boolean
    fun obtainBooks(file: File, handlers: Collection<BookHandler>): Flow<Pair<Book, Long>>
    fun supportsPath(path: String): Boolean
    fun obtainBook(path: String, handlers: Collection<BookHandler>): Book
    fun getData(path: String, handlers: Collection<BookHandler>): InputStream
    fun obtainBookSize(path: String): Long
}

interface FormatConverter : ExtensionPoint {
    val sourceFormat: String

    val targetFormat: String

    fun canConvert(sourceFormat: String): Boolean

    fun convert(inputStream: InputStream): File

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
