package com.github.asm0dey.opdsko_spring

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.net.URI
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import kotlin.math.min

@Component
class HtmxHandler(val repo: BookRepo, val service: BookService) {
    private val epubConverterAccessible: Boolean = false

    suspend fun homePage(req: ServerRequest): ServerResponse {
        val x = createHTML(false).div("grid") {
            NavTile("New books", "Recent publications from this catalog", "/api/new")
            NavTile("Books by series", "Authors by first letters", "/api/series/browse")
            NavTile("Books by author", "Series by first letters", "/api/author/c")
            NavTile("Genres", "Books by genres", "/api/genre")
        }
        return ok(smartHtml(req, x, BreadCrumbs("Library" to "/api")))
    }

    suspend fun search(req: ServerRequest): ServerResponse {
        val page = req.queryParamOrNull("page")?.toIntOrNull()?.minus(1) ?: 0
        val searchTerm = req.queryParam("search").getOrNull()!!
        val searchBookByText = repo.searchBookByText(searchTerm, page)
        println(searchBookByText)
        val x = createHTML(false).div("grid") {
            NavTile("New books", "Recent publications from this catalog", "/api/new")
            NavTile("Books by series", "Authors by first letters", "/api/series/browse")
            NavTile("Books by author", "Series by first letters", "/api/author/c")
            NavTile("Genres", "Books by genres", "/api/genre")
        }
        return ok(smartHtml(req, x, BreadCrumbs("Library" to "/api")))
    }

    suspend fun new(req: ServerRequest): ServerResponse {
        val page = req.pathVariableOrNull("page")?.toIntOrNull()?.minus(1) ?: 0
        val books = repo.newBooks(page)
        val imageTypes = service.imageTypes(books)
        val shortDescriptions = service.shortDescriptions(books)
        val x = createHTML(false).div("grid") {
            for (book in books) {
                BookTile(book, imageTypes, shortDescriptions)
            }
        }
        val y = BreadCrumbs(
            listOfNotNull(
                "Library" to "/api",
                "New" to "/api/new",
            )
        )
        return ok(smartHtml(req, x, y))
    }


    private suspend fun ok(responseData: String) =
        ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValueAndAwait(responseData)

    private fun smartHtml(
        call: ServerRequest,
        content: String,
        breadcrumbs: String,
        pagination: String = Pagination(1, 1, "")
    ) =
        if (call.headers().firstHeader("HX-Request").toBoolean()) content + breadcrumbs + pagination
        else fullHtml(breadcrumbs, content, pagination)

    private fun fullHtml(breadcrumbs: String, content: String, pagination: String = ""): String {
        return createHTML(false).html {
            head {
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                title("Asm0dey's library")
                link(rel = "stylesheet", href = "/webjars/bulma/1.0.2/css/bulma.min.css")
                link(rel = "stylesheet", href = "/webjars/font-awesome/4.7.0/css/font-awesome.min.css")

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
                nav(classes = "navbar") {
                    div("container") {
                        div("navbar-brand") {
                            a(classes = "navbar-item brand-text", href = "/api") {
                                attributes["hx-get"] = "/api"
                                attributes["hx-swap"] = "innerHTML show:.input:top"
                                attributes["hx-target"] = "#layout"
                                img(alt = "Logo", src = "/logo.png")
                                +Entities.nbsp
                                +"Asm0dey's library"
                            }
                        }
                    }
                }
                div("container") {
                    div("section") {
                        attributes["hx-boost"] = "true"
                        div("field has-addons") {
                            div("control has-icons-left is-expanded") {
                                input(InputType.text, name = "search", classes = "input") {
                                    attributes["placeholder"] = "Search"
                                    attributes["hx-trigger"] = "keyup[keyCode === 13]"
                                    attributes["hx-get"] = "/api/search"
                                    attributes["hx-swap"] = "innerHTML show:.input:top"
                                    attributes["hx-target"] = "#layout"
                                }
                                span("icon is-small is-left") {
                                    i("fa fa-search")
                                }
                            }
                            div("control") {
                                button(classes = "button") {
                                    attributes["hx-include"] = "[name='search']"
                                    attributes["hx-get"] = "/api/search"
                                    attributes["hx-swap"] = "innerHTML show:.input:top"
                                    attributes["hx-target"] = "#layout"
                                    +"Search"
                                }
                            }
                        }
                    }
                    nav("breadcrumb column is-12") {
                        attributes["aria-label"] = "breadcrumbs"
                        unsafe {
                            +breadcrumbs
                        }
                    }
                    div("fixed-grid column has-3-cols has-1-cols-mobile") {
                        id = "layout"
                        unsafe {
                            +content
                        }
                    }
                    div(classes = "navv") {
                        unsafe {
                            +pagination
                        }
                    }
                }
                div {
                    id = "modal-cont"
                }
                script(src = "/webjars/htmx.org/2.0.3/dist/htmx.min.js") {}
                script(src = "/webjars/hyperscript.org/0.9.12/dist/_hyperscript.min.js") {}
            }

        }
    }

    @Suppress("FunctionName")
    private fun DIV.NavTile(title: String, subtitle: String, href: String) {
        div("cell is-clickable") {
            layoutUpdateAttributes(href)
            article("box") {
                p("title") { +title }
                p("subtitle") { +subtitle }
            }
        }
    }

    @Suppress("FunctionName")
    private fun DIV.BookTile(
        bookWithInfo: BookWithInfo,
        images: Map<Long, String?>,
        descriptionsShort: Map<Long, String?>
    ) {
        div("cell") {
            div("card") {
                div("card-header") {
                    p("card-header-title") {
                        +bookWithInfo.book.name!!
                    }
                }
                if (images[bookWithInfo.id] != null) {
                    div("card-image") {
                        figure("image") {
                            a {
                                attributes["hx-get"] = "/api/book/${bookWithInfo.id}/image"
                                attributes["hx-swap"] = "innerHTML show:.input:top"
                                attributes["hx-target"] = "#modal-cont"
                                attributes["_"] = "on htmx:afterOnLoad wait 10ms then add .is-active to #modal"
                                img(src = "/opds/imag       e/${bookWithInfo.id}") {
                                    attributes["loading"] = "lazy"
                                }
                            }
                        }
                    }
                }
                div("card-content") {
                    div("content") {
                        text((descriptionsShort[bookWithInfo.id]?.let {
                            it.substring(0 until min(it.length, 200))
                        }?.plus('…') ?: ""))
                    }
                }
                footer("card-footer mb-0 pb-0 is-align-items-self-end") {
                    a(classes = "card-footer-item") {
                        attributes["hx-get"] = "/api/book/${bookWithInfo.id}/info"
                        attributes["hx-target"] = "#modal-cont"
                        attributes["_"] = "on htmx:afterOnLoad wait 10ms then add .is-active to #modal"
                        +"Info"
                    }
                    if (!epubConverterAccessible)
                        a("/opds/book/${bookWithInfo.id}/download", classes = "card-footer-item") { +"Download" }
                    else {
                        a("/opds/book/${bookWithInfo.id}/download", classes = "card-footer-item") { +"fb2" }
                        a("/opds/book/${bookWithInfo.id}/download/epub", classes = "card-footer-item") { +"epub" }
                    }
                }
            }
        }
    }

    @Suppress("FunctionName")
    private fun Pagination(curPage: Int, total: Int, base: String) = createHTML(false).div {
        fun String.withParam(param: String) = if (URI(this).query == null) "${this}?$param" else "${this}&$param"
        val last = total / 50 + 1
        attributes["hx-swap-oob"] = "innerHTML:.navv"
        if (curPage == 1 && total / 50 + 1 == 1) div()
        else
            nav {
                classes = setOf("pagination", "is-centered")
                role = "navigation"
                a {
                    classes = setOfNotNull("pagination-previous", if (curPage == 1) "is-disabled" else null)
                    if (curPage != 1) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = base.withParam("page=${curPage - 1}")
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                    }
                    +"Previous page"
                }
                a {
                    classes = setOfNotNull("pagination-next", if (curPage == last) "is-disabled" else null)
                    if (curPage != last) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = base.withParam("page=${curPage + 1}")
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                    }
                    +"Next page"
                }
                if (total != Int.MAX_VALUE) {
                    ul("pagination-list") {
                        val pageToDraw = (1..last).map {
                            it to (it == 1 || it == last || abs(curPage - it) <= 1)
                        }
                        val realToDraw = pageToDraw.fold(listOf<Pair<Int, Boolean>>()) { a, b ->
                            if (b.second) a + b
                            else if (a.last().second) a + (-1 to false)
                            else a
                        }
                        for ((page, draw) in realToDraw) {
                            if (!draw) {
                                li {
                                    span {
                                        classes = setOf("pagination-ellipsis")
                                        +"…"
                                    }
                                }
                            } else {
                                li {
                                    a {
                                        classes =
                                            setOfNotNull("pagination-link", if (curPage == page) "is-current" else null)
                                        attributes["hx-trigger"] = "click"
                                        attributes["hx-get"] = base.withParam("page=$page")
                                        attributes["hx-swap"] = "innerHTML show:.input:top"
                                        attributes["hx-target"] = "#layout"
                                        attributes["hx-push-url"] = "true"
                                        +(page.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    @Suppress("FunctionName")
    private fun BreadCrumbs(vararg items: Pair<String, String>) = BreadCrumbs(items.toList())

    @Suppress("FunctionName")
    private fun BreadCrumbs(items: List<Pair<String, String>>) = createHTML(false).div {
        attributes["hx-swap-oob"] = "innerHTML:.breadcrumb"
        ul {
            items.forEachIndexed { index, pair ->
                val (name, href) = pair
                li(if (index == items.size - 1) "is-active" else null) {
                    a {
                        layoutUpdateAttributes(href)
                        +name
                    }
                }
            }
        }
    }

    private fun HTMLTag.layoutUpdateAttributes(href: String) {
        attributes["hx-trigger"] = "click"
        attributes["hx-get"] = href
        attributes["hx-swap"] = "innerHTML show:.input:top"
        attributes["hx-target"] = "#layout"
        attributes["hx-push-url"] = "true"
    }

}