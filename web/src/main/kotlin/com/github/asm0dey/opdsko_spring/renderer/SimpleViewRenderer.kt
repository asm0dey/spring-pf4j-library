package com.github.asm0dey.opdsko_spring.renderer

import com.github.asm0dey.opdsko_spring.Book
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.min

@Component
class SimpleViewRenderer : ViewRenderer {
    override fun NavTile(title: String, subtitle: String, href: String): String {
        return createHTML(false).a(href = href, classes = "card") {
            style = "align-self: auto;"
            h3(classes = "card-title") { +title }
            p(classes = "card-text") { +subtitle }
        }
    }

    override fun BookTile(
        book: Book,
        images: Map<String, String?>,
        descriptions: Map<String, String?>,
        additionalFormats: List<String>
    ): String {
        return createHTML(false).div(classes = "card") {
            style = "align-self: auto;"
            div("section") {
                h3() { +book.name }
            }
            if (images[book.id] != null && book.hasCover) {
                a(href = "/simple/book/${book.id}/fullimage") {
                    img(src = "/common/image/${book.id}", classes = "section media") {
                        attributes["loading"] = "lazy"
                        attributes["alt"] = "Book cover for ${book.name}"
                        attributes["title"] = "Click to view full-size image"
                    }
                }
            }
            p() {
                text((descriptions[book.id]?.let {
                    it.substring(0 until min(it.length, 200))
                }?.plus('…') ?: ""))
            }
            div(classes = "btn-group section") {
                a(href = "/simple/book/${book.id}/info", classes = "button") { +"Info" }

                // Original format download
                val extension = book.path.substringAfterLast('.')
                a(href = "/common/book/${book.id}/download", classes = "button") {
                    +extension
                }

                additionalFormats.forEach { it ->
                    a(href = "/common/book/${book.id}/download/$it", classes = "button") {
                        +it
                    }
                }
            }
        }
    }

    override fun Breadcrumbs(items: List<Pair<String, String>>): String {
        return createHTML(false).div {
            ul(classes = "breadcrumb") {
                items.forEachIndexed { index, pair ->
                    val (name, href) = pair
                    li(classes = if (index == items.size - 1) "breadcrumb-item active" else "breadcrumb-item") {
                        a(href = href) { +name }
                    }
                }
            }
        }
    }


    override fun fullPage(content: String, breadcrumbs: String, pagination: String, fullRender: Boolean): String {
        return createHTML(false).html {
            head {
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                title("Asm0dey's library")
                link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/mini.css@3.0.1/dist/mini-default.min.css")
                style {
                    unsafe {
                        +"""
                             /* Custom styles for pagination */
    .pagination {
      display: flex;
      justify-content: center;
      list-style: none;
      padding: 0;
      margin: 20px 0;
    }

    .pagination li {
      margin: 0 5px;
    }

    .pagination a {
      display: block;
      padding: 8px 12px;
      text-decoration: none;
      border: 1px solid #ddd;
      border-radius: 4px;
      color: #333;
    }

    .pagination a:hover {
      background-color: #f0f0f0;
    }

    .pagination a.active {
      background-color: #007bff;
      color: white;
    }

    /* Responsive Design */
    @media (max-width: 600px) {
      .pagination li {
        display: none;
      }

      .pagination li:first-child,
      .pagination li:nth-child(2),
      .pagination li:last-child,
      .pagination li:nth-last-child(2) {
        display: inline-block;
      }
    }
                        """.trimIndent()
                    }
                }

                link(href = "/apple-touch-icon.png", rel = "apple-touch-icon") {
                    sizes = "180x180"
                }
                link(href = "/favicon-32x32.png", rel = "icon", type = "image/png") {
                    sizes = "32x32"
                }
                link(href = "/favicon-16x16.png", rel = "icon", type = "image/png") {
                    sizes = "16x16"
                }
                link(rel = "manifest", href = "/site.webmanifest")
                link(rel = "mask-icon", href = "/safari-pinned-tab.svg") {
                    attributes["color"] = "#5bbad5"
                }
                meta(name = "msapplication-TileColor", content = "#2b5797")
                meta(name = "theme-color", content = "#ffffff")
            }
            body {
                header(classes = "sticky") {
                    a(href = "/simple", classes = "logo") {
                        img(alt = "Logo", src = "/logo.png") {
                            width = "28"
                            height = "28"
                        }
                        +Entities.nbsp
                        +"Asm0dey's library"
                    }
                }
                div(classes = "container") {
                    div(classes = "row") {
                        div(classes = "col-sm-12") {
                            form(action = "/simple/search", method = FormMethod.get) {
                                div(classes = "row") {
                                    div(classes = "col-sm") {
                                        input(InputType.text, name = "search") {
                                            placeholder = "Search"
                                        }
                                    }
                                    div(classes = "col-sm-2") {
                                        button(type = ButtonType.submit, classes = "primary") {
                                            +"Search"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    div(classes = "row") {
                        div(classes = "col-sm-12") {
                            unsafe {
                                +breadcrumbs
                            }
                        }
                    }
                    div(classes = "row") {
                        unsafe {
                            +content
                        }
                    }
                    div(classes = "row") {
                        div(classes = "col-sm-12") {
                            unsafe {
                                +pagination
                            }
                        }
                    }
                }
            }
        }
    }

    override fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String {
        val curPage = currentPage
        val total = totalPages
        val base = baseUrl
        fun String.withParam(param: String) = if (URI(this).query == null) "${this}?$param" else "${this}&$param"
        val last = total / 15 + 1

        if (curPage == 1 && total / 15 + 1 == 1) return ""

        return createHTML(false).div {
            nav {
                ul(classes = "pagination") {
                    li(classes = if (curPage == 1) "page-item disabled" else "page-item") {
                        a(
                            href = if (curPage == 1) "#" else base.withParam("page=${curPage - 1}"),
                            classes = "page-link"
                        ) {
                            +"Previous"
                        }
                    }

                    val pageToDraw = (1..last).map {
                        it to (it == 1 || it == last || abs(curPage - it) <= 1)
                    }
                    val realToDraw = pageToDraw.fold(listOf<Pair<Int, Boolean>>()) { a, b ->
                        if (b.second) a + b
                        else if (a.isNotEmpty() && a.last().second) a + (-1 to false)
                        else a
                    }
                    for ((page, draw) in realToDraw) {
                        if (!draw) {
                            li(classes = "page-item") {
                                span(classes = "page-link") { +"…" }
                            }
                        } else {
                            li(classes = if (curPage == page) "page-item active" else "page-item") {
                                a(
                                    href = if (curPage == page) "#" else base.withParam("page=$page"),
                                    classes = "page-link"
                                ) {
                                    +(page.toString())
                                }
                            }
                        }
                    }

                    li(classes = if (curPage == last) "page-item disabled" else "page-item") {
                        a(
                            href = if (curPage == last) "#" else base.withParam("page=${curPage + 1}"),
                            classes = "page-link"
                        ) {
                            +"Next"
                        }
                    }
                }
            }
        }
    }

    override fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String {
        val curPage = currentPage
        val base = baseUrl
        fun String.withParam(param: String) = if (URI(this).query == null) "${this}?$param" else "${this}&$param"

        if (curPage == 1 && !hasMoreItems) return ""

        return createHTML(false).div {
            nav {
                ul(classes = "pagination") {
                    li(classes = if (curPage == 1) "page-item disabled" else "page-item") {
                        a(
                            href = if (curPage == 1) "#" else base.withParam("page=${curPage - 1}"),
                            classes = "page-link"
                        ) {
                            +"Previous"
                        }
                    }
                    li(classes = "page-item active") {
                        a(href = "#", classes = "page-link") {
                            +(curPage.toString())
                        }
                    }
                    li(classes = if (!hasMoreItems) "page-item disabled" else "page-item") {
                        a(
                            href = if (!hasMoreItems) "#" else base.withParam("page=${curPage + 1}"),
                            classes = "page-link"
                        ) {
                            +"Next"
                        }
                    }
                }
            }
        }
    }

    fun bookInfo(book: Book, description: String?, additionalFormats: List<String>, baseUrl: String): String {
        return createHTML(false).div {
            h1 { +book.name }

            if (book.hasCover) {
                figure {
                    a(href = "$baseUrl/book/${book.id}/fullimage") {
                        img(src = "/common/image/${book.id}", alt = "Book cover for ${book.name}")
                    }
                }
            }

            h2 { +"Book Information" }
            table {
                // Author information
                tr {
                    th { +"Author" }
                    td {
                        if (book.authors.isNotEmpty()) {
                            val author = book.authors[0]
                            val encodedAuthor = URLEncoder.encode(author.fullName, "UTF-8")
                            a(href = "$baseUrl/author/view/$encodedAuthor") { +author.fullName }
                        } else {
                            +"Unknown"
                        }
                    }
                }

                // Series information
                if (book.sequence != null) {
                    tr {
                        th { +"Series" }
                        td {
                            val encodedSeries = URLEncoder.encode(book.sequence, "UTF-8")
                            a(href = "$baseUrl/series/$encodedSeries") { +book.sequence }
                            if (book.sequenceNumber != null) {
                                +" (#${book.sequenceNumber})"
                            }
                        }
                    }
                }

                // Format information
                tr {
                    th { +"Format" }
                    td { +book.path.substringAfterLast('.').uppercase() }
                }
            }

            // Description
            h2 { +"Description" }
            if (description != null) {
                div { unsafe { +description } }
            } else {
                p { +"No description available." }
            }

            // Download links
            h2 { +"Download" }
            div {
                // Original format download
                a(href = "/common/book/${book.id}/download", classes = "button") {
                    +"Download ${book.path.substringAfterLast('.').uppercase()}"
                }

                additionalFormats.forEach {
                    a(href = "/common/book/${book.id}/download/$it", classes = "button") {
                        +"Download ${it.uppercase()}"
                    }
                }
            }
        }
    }

    fun fullBookCover(book: Book, baseUrl: String): String {
        return createHTML(false).div {
            h1 { +book.name }
            figure {
                img(src = "/common/fullimage/${book.id}", alt = "Full-size book cover for ${book.name}")
            }
            p {
                a(href = "$baseUrl/book/${book.id}/info", classes = "button") { +"Back to Book Info" }
            }
        }
    }
}
