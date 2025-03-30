package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.FormatConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

/**
 * A simple XML builder class that provides a more structured approach to building XML with StringBuilder.
 */
class XmlBuilder {
    private val sb = StringBuilder()
    private var indentLevel = 0
    private val indentString = "  "

    /**
     * Adds an XML declaration to the document.
     */
    fun xmlDeclaration(version: String = "1.0", encoding: String = "UTF-8") {
        sb.appendLine("""<?xml version="$version" encoding="$encoding"?>""")
    }

    /**
     * Adds an element with the given name, attributes, and content.
     */
    fun element(name: String, attributes: Map<String, String> = emptyMap(), content: String? = null, block: (XmlBuilder.() -> Unit)? = null) {
        indent()
        sb.append("<$name")
        attributes.forEach { (key, value) ->
            sb.append(" $key=\"$value\"")
        }
        if (content == null && block == null) {
            sb.appendLine("/>")
        } else {
            sb.append(">")
            if (content != null) {
                sb.append(content)
            }
            if (block != null) {
                sb.appendLine()
                indentLevel++
                block(this)
                indentLevel--
                indent()
            }
            sb.appendLine("</$name>")
        }
    }

    /**
     * Adds text content to the XML.
     */
    fun text(content: String) {
        sb.append(content)
    }

    /**
     * Returns the XML as a string.
     */
    override fun toString(): String {
        return sb.toString()
    }

    private fun indent() {
        repeat(indentLevel) {
            sb.append(indentString)
        }
    }
}

@Component
class OpdsHandler(
    val bookService: BookService,
    val formatConverters: List<FormatConverter>,
) {
    private val OPDS_MIME_TYPE = MediaType.parseMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")
    private val ISO_DATE_FORMAT = DateTimeFormatter.ISO_INSTANT

    suspend fun homePage(req: ServerRequest): ServerResponse {
        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Asm0dey's Library")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Navigation entries
            NavEntry(
                "New books", 
                "Recent publications from this catalog", 
                "/opds/new",
                "http://opds-spec.org/sort/new"
            )
            NavEntry(
                "Books by series", 
                "Authors by first letters", 
                "/opds/series/browse",
                "http://opds-spec.org/sort/popular"
            )
            NavEntry(
                "Books by author", 
                "Authors by first letters", 
                "/opds/author",
                "http://opds-spec.org/sort/popular"
            )
            NavEntry(
                "Genres", 
                "Books by genres", 
                "/opds/genre",
                "http://opds-spec.org/sort/popular"
            )
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    @Suppress("FunctionName")
    private fun XmlBuilder.NavEntry(title: String, summary: String, href: String, rel: String?) {
        element("entry") {
            element("title", content = title)
            element("id", content = href)
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("content", mapOf("type" to "text"), content = summary)

            val linkAttributes = mutableMapOf(
                "href" to href,
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation"
            )
            if (rel != null) {
                linkAttributes["rel"] = rel
            }
            element("link", linkAttributes)
        }
    }

    @Suppress("FunctionName")
    private fun XmlBuilder.BookEntry(book: Book, imageTypes: Map<String, String?>, descriptions: Map<String, String?>) {
        element("entry") {
            element("title", content = escapeXml(book.name))
            element("id", content = book.id!!)
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))

            // Add authors
            for (author in book.authors) {
                element("author") {
                    element("name", content = escapeXml(author.fullName))
                }
            }

            // Add description if available
            val description = descriptions[book.id]
            if (!description.isNullOrEmpty()) {
                element("summary", mapOf("type" to "text"), content = escapeXml(description))
            }

            // Add cover image link if available
            if (book.hasCover && imageTypes[book.id] != null) {
                element("link", mapOf(
                    "rel" to "http://opds-spec.org/image",
                    "href" to "/common/image/${book.id}",
                    "type" to "${imageTypes[book.id]}"
                ))
                element("link", mapOf(
                    "rel" to "http://opds-spec.org/image/thumbnail",
                    "href" to "/common/image/${book.id}",
                    "type" to "${imageTypes[book.id]}"
                ))
            }

            // Add acquisition links
            val extension = book.path.substringAfterLast('.')
            element("link", mapOf(
                "rel" to "http://opds-spec.org/acquisition",
                "href" to "/common/book/${book.id}/download",
                "type" to getMimeType(extension),
                "title" to "Download ${extension.uppercase()}"
            ))

            // Add conversion links if available
            val availableConverters = formatConverters.filter {
                it.sourceFormat.equals(extension, ignoreCase = true)
            }

            for (converter in availableConverters) {
                element("link", mapOf(
                    "rel" to "http://opds-spec.org/acquisition",
                    "href" to "/common/book/${book.id}/download/${converter.targetFormat}",
                    "type" to getMimeType(converter.targetFormat),
                    "title" to "Download ${converter.targetFormat.uppercase()}"
                ))
            }
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "epub" -> "application/epub+zip"
            "fb2" -> "application/x-fictionbook+xml"
            "pdf" -> "application/pdf"
            "mobi" -> "application/x-mobipocket-ebook"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    suspend fun search(req: ServerRequest): ServerResponse {
        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val searchTerm = req.queryParam("search").getOrNull()!!
        val books = bookService.searchBookByName(searchTerm, page)
        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Search: $searchTerm")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))

            // Pagination links
            if (pageParam > 1) {
                element("link", mapOf(
                    "rel" to "previous",
                    "href" to "/opds/search?search=$searchTerm&page=${pageParam - 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Previous Page"
                ))
            }

            // Assume there are more items if we have a full page of results
            val hasMoreItems = books.size == 15
            if (hasMoreItems) {
                element("link", mapOf(
                    "rel" to "next",
                    "href" to "/opds/search?search=$searchTerm&page=${pageParam + 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Next Page"
                ))
            }

            // Book entries
            for (book in books) {
                BookEntry(book, imageTypes, shortDescriptions)
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun new(req: ServerRequest): ServerResponse {
        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val pagedBooks = bookService.newBooks(page)
        val books = pagedBooks.books
        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "New Books")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))

            // Pagination links
            if (pageParam > 1) {
                element("link", mapOf(
                    "rel" to "previous",
                    "href" to "/opds/new?page=${pageParam - 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Previous Page"
                ))
            }

            // Assume there are more items if we have a full page of results
            val hasMoreItems = books.size >= 24
            if (hasMoreItems) {
                element("link", mapOf(
                    "rel" to "next",
                    "href" to "/opds/new?page=${pageParam + 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Next Page"
                ))
            }

            // Book entries
            for (book in books) {
                BookEntry(book, imageTypes, shortDescriptions)
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    /**
     * Handler for author navigation - first level (first letters)
     */
    suspend fun authorFirstLevel(req: ServerRequest): ServerResponse {
        val letters = bookService.findAuthorFirstLetters().toList()

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Authors")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))

            // Author first letter entries
            for (letter in letters) {
                val title = if (letter.count > 1) letter.id else letter.id
                NavEntry(
                    title, 
                    "Authors starting with ${letter.id} (${letter.count})", 
                    "/opds/author/${letter.id}",
                    null
                )
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    /**
     * Handler for author navigation - prefix level (first N letters)
     */
    suspend fun authorPrefixLevel(req: ServerRequest): ServerResponse {
        val prefix = req.pathVariable("prefix")
        val depth = prefix.length

        // If depth is 5 or more, show full author names
        if (depth >= 5) {
            return authorFullNames(req)
        }

        // Check if the prefix matches any complete last names
        val exactLastNameCount = bookService.countExactLastNames(prefix).toList()
        if (exactLastNameCount.isNotEmpty() && exactLastNameCount[0].count > 0) {
            return authorFullNames(req)
        }

        val nextDepth = depth + 1
        val prefixes = bookService.findAuthorPrefixes(prefix, nextDepth).toList()

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Authors: $prefix")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to parent
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))

            // Build up link
            val upLink = if (prefix.length > 1) {
                "/opds/author/${prefix.substring(0, prefix.length - 1)}"
            } else {
                "/opds/author"
            }
            element("link", mapOf(
                "rel" to "up",
                "href" to upLink,
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Up"
            ))

            // Author prefix entries
            for (prefixResult in prefixes) {
                val title = if (prefixResult.count > 1) "${prefixResult.id} (${prefixResult.count})" else prefixResult.id
                NavEntry(
                    title, 
                    "Authors starting with ${prefixResult.id}", 
                    "/opds/author/${URLEncoder.encode(prefixResult.id, "UTF-8")}",
                    null
                )
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun authorFullNames(req: ServerRequest): ServerResponse {
        val prefix = req.pathVariable("prefix")
        val authors = bookService.findAuthorsByPrefix(prefix).toList()

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Authors: $prefix")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to parent
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))

            // Build up link
            val upLink = if (prefix.length > 1) {
                "/opds/author/${prefix.substring(0, prefix.length - 1)}"
            } else {
                "/opds/author"
            }
            element("link", mapOf(
                "rel" to "up",
                "href" to upLink,
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Up"
            ))

            // Author entries
            for (author in authors) {
                val fullName = author.id.fullName
                val encodedFullName = URLEncoder.encode(fullName, "UTF-8")
                NavEntry(
                    fullName,
                    "View books by this author",
                    "/opds/author/view/$encodedFullName",
                    null
                )
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun authorView(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val encodedFullName = req.pathVariable("fullName")
        // Check if the author has any series
        val seriesExist = bookService.findSeriesByAuthorFullName(fullName).firstOrNull() != null

        // If the author doesn't have any series, redirect directly to the "All Books" view
        if (!seriesExist) return authorAllBooks(req)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = fullName)
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds/author",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Authors"
            ))

            // Navigation entries
            NavEntry(
                "By Series",
                "View series by this author",
                "/opds/author/view/$encodedFullName/series",
                null
            )
            NavEntry(
                "Without Series",
                "View books without series",
                "/opds/author/view/$encodedFullName/noseries",
                null
            )
            NavEntry(
                "All Books",
                "View all books by this author",
                "/opds/author/view/$encodedFullName/all",
                null
            )
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun authorSeries(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }

        val series = bookService.findSeriesByAuthorFullName(fullName).toList()

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Series by $fullName")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to author
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds/author/view/$encodedFullName",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to fullName
            ))

            // Series entries
            for (seriesResult in series) {
                val encodedSeries = URLEncoder.encode(seriesResult.id, "UTF-8")
                NavEntry(
                    seriesResult.id,
                    "View books in this series",
                    "/opds/author/view/$encodedFullName/series/$encodedSeries",
                    null
                )
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun authorSeriesBooks(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val seriesName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("series"), "UTF-8")
        }
        val encodedFullName = withContext(Dispatchers.IO) {
            URLEncoder.encode(fullName, "UTF-8")
        }

        val sort = Sort.by(Sort.Direction.ASC, "sequenceNumber")
        val books = bookService.findBooksBySeries(seriesName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "$seriesName by $fullName")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to series
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds/author/view/$encodedFullName/series",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Series by $fullName"
            ))

            // Book entries
            for (book in books) {
                BookEntry(book, imageTypes, shortDescriptions)
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun authorNoSeriesBooks(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }

        val sort = Sort.by(Sort.Direction.ASC, "name")
        val books = bookService.findBooksByAuthorWithoutSeriesFullName(fullName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Books without series by $fullName")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to author
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds/author/view/$encodedFullName",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to fullName
            ))

            // Book entries
            for (book in books) {
                BookEntry(book, imageTypes, shortDescriptions)
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun authorAllBooks(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }

        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val pagedBooks = bookService.findBooksByAuthorFullName(fullName, page)
        val books = pagedBooks.books

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "All books by $fullName")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to author
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds/author/view/$encodedFullName",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to fullName
            ))

            // Pagination links
            if (pageParam > 1) {
                element("link", mapOf(
                    "rel" to "previous",
                    "href" to "/opds/author/view/$encodedFullName/all?page=${pageParam - 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Previous Page"
                ))
            }

            // Check if there are more pages
            val totalPages = (pagedBooks.total / 24) + 1
            if (pageParam < totalPages) {
                element("link", mapOf(
                    "rel" to "next",
                    "href" to "/opds/author/view/$encodedFullName/all?page=${pageParam + 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Next Page"
                ))
            }

            // Book entries
            for (book in books) {
                BookEntry(book, imageTypes, shortDescriptions)
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }

    suspend fun seriesBooks(req: ServerRequest): ServerResponse {
        val seriesName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("series"), "UTF-8")
        }

        val sort = Sort.by(Sort.Direction.ASC, "sequenceNumber")
        val books = bookService.findBooksBySeries(seriesName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element("feed", mapOf(
            "xmlns" to "http://www.w3.org/2005/Atom",
            "xmlns:opds" to "http://opds-spec.org/2010/catalog"
        )) {
            element("id", content = "urn:uuid:${req.uri()}")
            element("title", content = "Series: $seriesName")
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Link back to root and up to series
            element("link", mapOf(
                "rel" to "start",
                "href" to "/opds",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Home"
            ))
            element("link", mapOf(
                "rel" to "up",
                "href" to "/opds/series",
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                "title" to "Series"
            ))

            // Book entries
            for (book in books) {
                BookEntry(book, imageTypes, shortDescriptions)
            }
        }

        return ok()
            .contentType(OPDS_MIME_TYPE)
            .bodyValueAndAwait(xmlBuilder.toString())
    }
}
