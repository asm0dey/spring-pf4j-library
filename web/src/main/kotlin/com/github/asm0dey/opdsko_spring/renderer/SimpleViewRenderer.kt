package com.github.asm0dey.opdsko_spring.renderer

import com.github.asm0dey.opdsko_spring.Book
import com.github.asm0dey.opdsko_spring.LibraryProperties
import kotlinx.html.*
import kotlinx.html.ButtonType.submit
import kotlinx.html.FormMethod.post
import kotlinx.html.stream.createHTML
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.min

@Component
class SimpleViewRenderer(private val libraryProperties: LibraryProperties) : ViewRenderer {
    override fun NavTile(title: String, subtitle: String, href: String): String {
        return createHTML(false).div(classes = "column col-4") {
            div("card") {
                div(classes = "card-header") {
                    div(classes = "card-title h5") { +title }
                }
                div(classes = "card-body") {
                    p { +subtitle }
                }
                div(classes = "card-footer") {
                    a(href = href, classes = "btn btn-primary") { +"View" }
                }
            }
        }
    }

    override fun BookTile(
        book: Book,
        images: Map<String, String?>,
        descriptions: Map<String, String?>,
        additionalFormats: List<String>
    ): String {
        return createHTML(false).div(classes = "column col-4") {
            div("card") {
                div(classes = "card-image") {
                    a(href = "/simple/book/${book.id}/fullimage") {
                        img(src = "/common/image/${book.id}", classes = "img-responsive") {
                            attributes["loading"] = "lazy"
                            attributes["alt"] = "Book cover for ${book.name}"
                            attributes["title"] = "Click to view full-size image"
                        }
                    }
                }
                div(classes = "card-header") {
                    div(classes = "card-title h5") { +book.name }
                }
                div(classes = "card-body") {
                    p {
                        text(
                            (descriptions[book.id]?.let { it.substring(0 until min(it.length, 200)) }?.plus('…') ?: "")
                        )
                    }
                }
                div(classes = "card-footer") {
                    a(href = "/simple/book/${book.id}/info", classes = "btn btn-primary") { +"Info" }

                    // Original format download
                    val extension = book.path.substringAfterLast('.')
                    a(href = "/common/book/${book.id}/download", classes = "btn btn-link") {
                        +extension
                    }

                    additionalFormats.forEach {
                        a(href = "/common/book/${book.id}/download/$it", classes = "btn btn-link") {
                            +it
                        }
                    }
                }
            }
        }
    }

    override fun Breadcrumbs(items: List<Pair<String, String>>): String {
        return createHTML(false).ul(classes = "breadcrumb") {
            items.forEachIndexed { index, pair ->
                val (name, href) = pair
                li(classes = (if (index == items.size - 1) "active " else "") + "breadcrumb-item") {
                    a(href = href) { +name }
                }
            }
        }
    }


    override fun fullPage(
        content: String,
        breadcrumbs: String,
        pagination: String,
        fullRender: Boolean,
        isAdmin: Boolean
    ): String {
        return createHTML(false).html {
            head {
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                title(libraryProperties.title)
                link(rel = "stylesheet", href = "/webjars/spectre.css/0.5.9/dist/spectre.min.css")
                link(rel = "stylesheet", href = "/webjars/spectre.css/0.5.9/dist/spectre-icons.min.css")
                style {
                    unsafe {
                        +"""
                            .books .col-4 {
                                padding: .5em;
                            }
                            .books .card {
                                border: 0;
                                box-shadow: 0 .25rem 1rem rgba(48,55,66,.15);
                                height: 100%;
                            }
                            .books .card img {
                                width: 100%;
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
            body("books") {
                header(classes = "navbar") {
                    section(classes = "navbar-section") {
                        a(href = "/simple", classes = "navbar-brand mr-2") {
                            img(alt = "Logo", src = "/logo.png") {
                                width = "28"
                                height = "28"
                            }
                            +Entities.nbsp
                            +libraryProperties.title
                        }
                    }
                    section(classes = "navbar-section") {
                        if (isAdmin) {
                            form(action = "/cleanup", method = post, classes = "d-inline") {
                                a(href = "javascript:;", classes = "btn btn-link") {
                                    onClick = "parentNode.submit();"
                                    +"Scan"
                                }
                            }
                            form(action = "/cleanup", method = post, classes = "d-inline") {
                                a(href = "javascript:;", classes = "btn btn-link") {
                                    onClick = "parentNode.submit();"
                                    +"Resync"
                                }

                            }
                            form(action = "/cleanup", method = post, classes = "d-inline") {
                                a(href = "javascript:;", classes = "btn btn-link") {
                                    onClick = "parentNode.submit();"
                                    +"Cleanup"
                                }

                            }
                        }
                        form(action = "/logout", method = post, classes = "d-inline") {
                            a(href = "javascript:;", classes = "btn btn-link") {
                                onClick = "parentNode.submit();"
                                +"Logout"
                            }
                        }
                    }
                }
                div(classes = "container grid-lg") {
                    div(classes = "columns") {
                        div(classes = "column col-12") {
                            form(action = "/simple/search", method = FormMethod.get) {
                                div(classes = "input-group") {
                                    input(InputType.text, name = "search", classes = "form-input") {
                                        placeholder = "Search"
                                    }
                                    button(type = submit, classes = "btn btn-primary input-group-btn") {
                                        +"Search"
                                    }
                                }
                            }
                        }
                    }
                    div(classes = "columns") {
                        div(classes = "column col-12") {
                            unsafe {
                                +breadcrumbs
                            }
                        }
                    }
                    div(classes = "columns") {
                        unsafe {
                            +content
                        }
                    }
                    div(classes = "columns") {
                        div(classes = "column col-12") {
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
        fun String.withParam(param: String) = if (URI(this).query == null) "${this}?$param" else "${this}&$param"
        val last = totalPages / 15 + 1

        if (currentPage == 1 && totalPages / 15 + 1 == 1) return ""

        return createHTML(false).div {
            ul(classes = "pagination") {
                li(classes = if (currentPage == 1) "page-item disabled" else "page-item") {
                    a(
                        href = if (currentPage == 1) "#" else baseUrl.withParam("page=${currentPage - 1}")
                    ) {
                        +"Previous"
                    }
                }

                val pageToDraw = (1..last).map {
                    it to (it == 1 || it == last || abs(currentPage - it) <= 1)
                }
                val realToDraw = pageToDraw.fold(listOf<Pair<Int, Boolean>>()) { a, b ->
                    if (b.second) a + b
                    else if (a.isNotEmpty() && a.last().second) a + (-1 to false)
                    else a
                }
                for ((page, draw) in realToDraw) {
                    if (!draw) {
                        li(classes = "page-item") {
                            span { +"…" }
                        }
                    } else {
                        li(classes = if (currentPage == page) "page-item active" else "page-item") {
                            a(
                                href = if (currentPage == page) "#" else baseUrl.withParam("page=$page")
                            ) {
                                +(page.toString())
                            }
                        }
                    }
                }

                li(classes = if (currentPage == last) "page-item disabled" else "page-item") {
                    a(
                        href = if (currentPage == last) "#" else baseUrl.withParam("page=${currentPage + 1}")
                    ) {
                        +"Next"
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
            ul(classes = "pagination") {
                li(classes = if (curPage == 1) "page-item disabled" else "page-item") {
                    a(
                        href = if (curPage == 1) "#" else base.withParam("page=${curPage - 1}")
                    ) {
                        +"Previous"
                    }
                }
                li(classes = "page-item active") {
                    a(href = "#") {
                        +(curPage.toString())
                    }
                }
                li(classes = if (!hasMoreItems) "page-item disabled" else "page-item") {
                    a(
                        href = if (!hasMoreItems) "#" else base.withParam("page=${curPage + 1}")
                    ) {
                        +"Next"
                    }
                }
            }
        }
    }

    fun bookInfo(book: Book, description: String?, additionalFormats: List<String>, baseUrl: String): String {
        return createHTML(false).div {
            h1(classes = "text-center") { +book.name }

            if (book.hasCover) {
                figure(classes = "figure") {
                    a(href = "$baseUrl/book/${book.id}/fullimage") {
                        img(
                            src = "/common/image/${book.id}",
                            alt = "Book cover for ${book.name}",
                            classes = "img-responsive"
                        )
                    }
                }
            }

            h2 { +"Book Information" }
            table(classes = "table table-striped table-hover") {
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
                div(classes = "content") { unsafe { +description } }
            } else {
                p { +"No description available." }
            }

            // Download links
            h2 { +"Download" }
            div(classes = "btn-group btn-group-block") {
                // Original format download
                a(href = "/common/book/${book.id}/download", classes = "btn btn-primary") {
                    +"Download ${book.path.substringAfterLast('.').uppercase()}"
                }

                additionalFormats.forEach {
                    a(href = "/common/book/${book.id}/download/$it", classes = "btn") {
                        +"Download ${it.uppercase()}"
                    }
                }
            }
        }
    }

    fun fullBookCover(book: Book, baseUrl: String): String {
        return createHTML(false).div {
            h1(classes = "text-center") { +book.name }
            figure(classes = "figure") {
                img(
                    src = "/common/fullimage/${book.id}",
                    alt = "Full-size book cover for ${book.name}",
                    classes = "img-responsive"
                )
            }
            p(classes = "text-center") {
                a(href = "$baseUrl/book/${book.id}/info", classes = "btn btn-primary") { +"Back to Book Info" }
            }
        }
    }
}
