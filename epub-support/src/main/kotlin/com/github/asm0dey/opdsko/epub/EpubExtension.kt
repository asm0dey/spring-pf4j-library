package com.github.asm0dey.opdsko.epub

import com.github.asm0dey.opdsko.common.Author
import com.github.asm0dey.opdsko.common.AuthorAdapter
import com.github.asm0dey.opdsko.common.Book
import com.github.asm0dey.opdsko.common.BookHandler
import io.documentnode.epub4j.epub.EpubReader
import org.pf4j.Extension
import java.io.InputStream
import io.documentnode.epub4j.domain.Book as Epub4JBook


@Extension(points = [BookHandler::class])
class EpubBookHandler : BookHandler {
    override fun supportsFile(fileName: String, data: () -> InputStream) =
        fileName.lowercase().endsWith(".epub")

    override fun bookInfo(fileName: String, dataProvider: () -> InputStream): Book =
        dataProvider().buffered().use { EpubBook(EpubReader().readEpub(it), fileName) }

    override val readFormats: List<String> = listOf("epub")

}

private data class EpubBook(
    override val title: String,
    override val cover: ByteArray?,
    override val coverContentType: String?,
    override val annotation: String?,
    override val authors: List<Author>,
    override val genres: List<String>,
    override val sequenceName: String?,
    override val sequenceNumber: Int?,
    override val path: String,
) : Book {
    constructor(book: Epub4JBook, path: String) : this(
        title = book.title,
        cover = book.coverImage?.data,
        coverContentType = book.coverImage.mediaType.name,
        annotation = book.metadata.descriptions.firstOrNull(),
        authors = book.metadata.authors.map {
            AuthorAdapter(
                firstName = it.firstname,
                lastName = it.lastname
            )
        },
        genres = book.metadata.subjects,
        sequenceName = book.metadata.getMetaAttribute("calibre:series"),
        sequenceNumber = book.metadata.getMetaAttribute("calibre:series_index")?.toDoubleOrNull()?.toInt(),
        path = path,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpubBook

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
        return result
    }


}
