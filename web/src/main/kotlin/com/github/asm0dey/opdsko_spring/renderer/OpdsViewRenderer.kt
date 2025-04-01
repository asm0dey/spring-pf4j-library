package com.github.asm0dey.opdsko_spring.renderer

import com.github.asm0dey.opdsko_spring.Book
import com.github.asm0dey.opdsko_spring.LibraryProperties
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class OpdsViewRenderer(private val libraryProperties: LibraryProperties) : ViewRenderer {
    private val ISO_DATE_FORMAT = DateTimeFormatter.ISO_INSTANT

    override fun NavTile(title: String, subtitle: String, href: String): String {
        val xmlBuilder = XmlBuilder()
        xmlBuilder.element("entry") {
            element("title", content = title)
            element("id", content = href)
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("content", mapOf("type" to "text"), content = subtitle)

            val linkAttributes = mutableMapOf(
                "href" to href,
                "type" to "application/atom+xml;profile=opds-catalog;kind=navigation"
            )
            element("link", linkAttributes)
        }
        return xmlBuilder.toString()
    }

    override fun BookTile(
        book: Book, images: Map<String, String?>, descriptions: Map<String, String?>, additionalFormats: List<String>
    ): String {
        val xmlBuilder = XmlBuilder()
        xmlBuilder.element("entry") {
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
            if (book.hasCover && images[book.id] != null) {
                element(
                    "link", mapOf(
                        "rel" to "http://opds-spec.org/image",
                        "href" to "/common/image/${book.id}",
                        "type" to "${images[book.id]}"
                    )
                )
                element(
                    "link", mapOf(
                        "rel" to "http://opds-spec.org/image/thumbnail",
                        "href" to "/common/image/${book.id}",
                        "type" to "${images[book.id]}"
                    )
                )
            }

            // Add acquisition link for original format
            val extension = book.path.substringAfterLast('.')
            element(
                "link", mapOf(
                    "rel" to "http://opds-spec.org/acquisition",
                    "href" to "/common/book/${book.id}/download",
                    "type" to getMimeType(extension),
                    "title" to "Download ${extension.uppercase()}"
                )
            )

            additionalFormats.forEach { it ->
                element(
                    "link", mapOf(
                        "rel" to "http://opds-spec.org/acquisition",
                        "href" to "/common/book/${book.id}/download/$it",
                        "type" to getMimeType(it),
                        "title" to "Download $it"
                    )
                )
            }
        }
        return xmlBuilder.toString()
    }

    override fun Breadcrumbs(items: List<Pair<String, String>>): String {
        val xmlBuilder = XmlBuilder()

        // Add navigation links
        for ((name, href) in items) {
            if (href != items.last().second) { // Skip the current page
                xmlBuilder.element(
                    "link", mapOf(
                        "rel" to if (href == items.first().second) "start" else "up",
                        "href" to href,
                        "type" to "application/atom+xml;profile=opds-catalog;kind=navigation",
                        "title" to name
                    )
                )
            }
        }

        return xmlBuilder.toString()
    }


    override fun fullPage(content: String, breadcrumbs: String, pagination: String, fullRender: Boolean, isAdmin: Boolean): String {
        val xmlBuilder = XmlBuilder()
        xmlBuilder.xmlDeclaration()

        xmlBuilder.element(
            "feed", mapOf(
                "xmlns" to "http://www.w3.org/2005/Atom",
                "xmlns:opds" to "http://opds-spec.org/2010/catalog"
            )
        ) {
            element("id", content = "urn:uuid:opdsko-catalog")
            element("title", content = libraryProperties.title)
            element("updated", content = ZonedDateTime.now(ZoneId.of("UTC")).format(ISO_DATE_FORMAT))
            element("author") {
                element("name", content = "Asm0dey")
            }

            // Add breadcrumbs
            text(breadcrumbs)

            // Add pagination
            text(pagination)

            // Add content
            text(content)
        }

        return xmlBuilder.toString()
    }

    override fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String {
        val xmlBuilder = XmlBuilder()

        // Previous page link
        if (currentPage > 1) {
            xmlBuilder.element(
                "link", mapOf(
                    "rel" to "previous",
                    "href" to "$baseUrl?page=${currentPage - 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Previous Page"
                )
            )
        }

        // Next page link
        val last = totalPages / 15 + 1
        if (currentPage < last) {
            xmlBuilder.element(
                "link", mapOf(
                    "rel" to "next",
                    "href" to "$baseUrl?page=${currentPage + 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Next Page"
                )
            )
        }

        return xmlBuilder.toString()
    }

    override fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String {
        val xmlBuilder = XmlBuilder()

        // Previous page link
        if (currentPage > 1) {
            xmlBuilder.element(
                "link", mapOf(
                    "rel" to "previous",
                    "href" to "$baseUrl?page=${currentPage - 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Previous Page"
                )
            )
        }

        // Next page link
        if (hasMoreItems) {
            xmlBuilder.element(
                "link", mapOf(
                    "rel" to "next",
                    "href" to "$baseUrl?page=${currentPage + 1}",
                    "type" to "application/atom+xml;profile=opds-catalog;kind=acquisition",
                    "title" to "Next Page"
                )
            )
        }

        return xmlBuilder.toString()
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
}

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
    fun element(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        content: String? = null,
        block: (XmlBuilder.() -> Unit)? = null
    ) {
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
