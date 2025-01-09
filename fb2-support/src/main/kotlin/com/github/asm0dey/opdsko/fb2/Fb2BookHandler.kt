package com.github.asm0dey.opdsko.fb2

import com.github.asm0dey.opdsko.common.AuthorAdapter
import com.github.asm0dey.opdsko.common.Book
import com.github.asm0dey.opdsko.common.BookHandler
import fb2.FictionBook
import org.pf4j.Extension
import java.io.InputStream

@Extension(points = [BookHandler::class])
class Fb2BookHandler : BookHandler {
    override fun supportsFile(fileName: String, data: () -> InputStream) = fileName.lowercase().endsWith(".fb2")

    override fun bookInfo(fileName: String, dataProvider: () -> InputStream): Book =
        Fb2Book(FictionBook(fileName, dataProvider), fileName)

    override val readFormats: List<String>
        get() = listOf("fb2") + if (Fb2Plugin.epubConverterAccessible) listOf("epub") else listOf()
}

private data class Fb2Book(
    override val title: String,
    @Transient override val cover: ByteArray?,
    override val coverContentType: String?,
    override val annotation: String?,
    override val authors: List<AuthorAdapter>,
    override val genres: List<String>,
    override val sequenceName: String?,
    override val sequenceNumber: Int?,
    override val path: String,
) : Book {

    constructor(fb: FictionBook, path: String) : this(
        title = fb.title,
        cover = fb.binaries[fb.description?.titleInfo?.coverPage?.first()?.value?.replace("#", "")]?.binary,
        coverContentType = fb.description?.titleInfo?.coverPage?.firstOrNull()?.value?.replace("#", "")?.let {
            fb.binaries[it]?.contentType
        },
        annotation = fb.description?.titleInfo?.annotation?.text,
        authors = fb.description?.titleInfo?.authors?.map {
            AuthorAdapter(
                it.firstName,
                it.lastName,
                it.middleName,
                it.nickname
            )
        } ?: listOf(),
        genres = fb.description?.titleInfo?.genres ?: listOf(),
        sequenceName = fb.description?.titleInfo?.sequence?.name,
        sequenceNumber = fb.description?.titleInfo?.sequence?.number?.toIntOrNull(),
        path = path,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fb2Book

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