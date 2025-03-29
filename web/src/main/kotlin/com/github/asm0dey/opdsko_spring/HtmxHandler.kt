package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.FormatConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import kotlin.math.min

@Component
class HtmxHandler(
    val bookService: BookService,
    val formatConverters: List<FormatConverter>,
    val seaweedFSService: SeaweedFSService
) {

    suspend fun homePage(req: ServerRequest): ServerResponse {
        val x = createHTML(false).div("grid") {
            NavTile("New books", "Recent publications from this catalog", "/api/new")
            NavTile("Books by series", "Authors by first letters", "/api/series/browse")
            NavTile("Books by author", "Authors by first letters", "/api/author")
            NavTile("Genres", "Books by genres", "/api/genre")
        }
        return ok(smartHtml(req, x, BreadCrumbs("Library" to "/api")))
    }

    suspend fun search(req: ServerRequest): ServerResponse {
        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val searchTerm = req.queryParam("search").getOrNull()!!
        val books = bookService.searchBookByName(searchTerm, page)
        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)
        val x = createHTML(false).div("grid") {
            for (book in books) {
                BookTile(book, imageTypes, shortDescriptions)
            }
        }
        val y = BreadCrumbs(
            listOfNotNull(
                "Library" to "/api",
                "Search: $searchTerm" to "/api/search?search=$searchTerm",
            )
        )
        // Assume there are more items if we have a full page of results
        val hasMoreItems = books.size == 15
        val pagination = IndeterminatePagination(pageParam, hasMoreItems, "/api/search?search=$searchTerm")
        return ok(smartHtml(req, x, y, pagination))
    }

    suspend fun new(req: ServerRequest): ServerResponse {
        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val pagedBooks = bookService.newBooks(page)
        val books = pagedBooks.books
        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)
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
        // Assume there are more items if we have a full page of results
        val hasMoreItems = books.size >= 24
        val pagination = IndeterminatePagination(pageParam, hasMoreItems, "/api/new")
        return ok(smartHtml(req, x, y, pagination))
    }


    private suspend fun ok(responseData: String) =
        ok().contentType(MediaType.TEXT_HTML).bodyValueAndAwait(responseData)

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
                link(rel = "stylesheet", href = "/webjars/bulma/1.0.3/css/bulma.min.css")
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
                div("modal") {
                    id = "modal"
                    div("modal-background") {
                        attributes["_"] = "on click remove .is-active from #modal"
                    }
                    div("modal-content") {
                        id = "modal-content"
                    }
                    button(classes = "modal-close is-large") {
                        attributes["aria-label"] = "close"
                        attributes["_"] = "on click remove .is-active from #modal"
                    }
                }
                script(src = "/webjars/htmx.org/2.0.4/dist/htmx.min.js") {}
                script(src = "/webjars/htmx-ext-sse/2.2.3/dist/sse.min.js") {}
                script(src = "/webjars/hyperscript.org/0.9.13/dist/_hyperscript.min.js") {}
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
        bookWithInfo: Book,
        images: Map<String, String?>,
        descriptionsShort: Map<String, String?>
    ) {
        div("cell") {
            div("card") {
                div("card-header") {
                    p("card-header-title") {
                        +bookWithInfo.name
                    }
                }
                if (images[bookWithInfo.id] != null && bookWithInfo.hasCover) {
                    div("card-image") {
                        figure("image") {
                            a {
                                attributes["hx-get"] = "/api/book/${bookWithInfo.id}/image"
                                attributes["hx-swap"] = "innerHTML"
                                attributes["hx-target"] = "#modal-content"
                                attributes["_"] = "on htmx:afterOnLoad wait 10ms then add .is-active to #modal"
                                // Use the preview image by default
                                img(src = "/opds/image/${bookWithInfo.id}") {
                                    attributes["loading"] = "lazy"
                                    attributes["alt"] = "Book cover for ${bookWithInfo.name}"
                                    attributes["title"] = "Click to view full-size image"
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
                        attributes["hx-target"] = "#modal-content"
                        attributes["hx-swap"] = "innerHTML"
                        attributes["_"] = "on htmx:afterOnLoad wait 10ms then add .is-active to #modal"
                        +"Info"
                    }
                    // Check if there are any format converters available for this book
                    val availableConverters = formatConverters.filter {
                        it.sourceFormat.equals(
                            bookWithInfo.path.substringAfterLast('.'),
                            ignoreCase = true
                        )
                    }
                    if (availableConverters.isEmpty()) {
                        a("/opds/book/${bookWithInfo.id}/download", classes = "card-footer-item") { +"Download" }
                    } else {
                        // Original format download
                        a(
                            "/opds/book/${bookWithInfo.id}/download",
                            classes = "card-footer-item"
                        ) { +bookWithInfo.path.substringAfterLast('.') }
                        // Converted format downloads
                        availableConverters.forEach { converter ->
                            a(
                                "/opds/book/${bookWithInfo.id}/download/${converter.targetFormat}",
                                classes = "card-footer-item"
                            ) { +converter.targetFormat }
                        }
                    }
                }
            }
        }
    }

    @Suppress("FunctionName")
    private fun Pagination(curPage: Int, total: Int, base: String) = createHTML(false).div {
        fun String.withParam(param: String) = if (URI(this).query == null) "${this}?$param" else "${this}&$param"
        val last = total / 15 + 1
        attributes["hx-swap-oob"] = "innerHTML:.navv"
        if (curPage == 1 && total / 15 + 1 == 1) div()
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
    private fun IndeterminatePagination(curPage: Int, hasMoreItems: Boolean, base: String) = createHTML(false).div {
        fun String.withParam(param: String) = if (URI(this).query == null) "${this}?$param" else "${this}&$param"
        attributes["hx-swap-oob"] = "innerHTML:.navv"
        if (curPage == 1 && !hasMoreItems) div()
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
                    classes = setOfNotNull("pagination-next", if (!hasMoreItems) "is-disabled" else null)
                    if (hasMoreItems) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = base.withParam("page=${curPage + 1}")
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                    }
                    +"Next page"
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

    /**
     * Handler for author navigation - first level (first letters)
     */
    suspend fun authorFirstLevel(req: ServerRequest): ServerResponse {
        val letters = bookService.findAuthorFirstLetters().toList()
        val x = createHTML(false).div("grid") {
            for (letter in letters) {
                val title = if (letter.count > 1) letter.id else letter.id
                NavTile(title, "Authors starting with ${letter.id}  (${letter.count})", "/api/author/${letter.id}")
            }
        }
        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author"
            )
        )
        return ok(smartHtml(req, x, breadcrumbs))
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

        val x = createHTML(false).div("grid") {
            for (prefixResult in prefixes) {
                val title =
                    if (prefixResult.count > 1) "${prefixResult.id} (${prefixResult.count})" else prefixResult.id
                NavTile(
                    title,
                    "Authors starting with ${prefixResult.id}",
                    "/api/author/${URLEncoder.encode(prefixResult.id, "UTF-8")}"
                )
            }
        }

        val breadcrumbs = buildAuthorBreadcrumbs(prefix)
        return ok(smartHtml(req, x, breadcrumbs))
    }

    suspend fun authorFullNames(req: ServerRequest): ServerResponse {
        val prefix = req.pathVariable("prefix")
        val authors = bookService.findAuthorsByPrefix(prefix).toList()

        val x = createHTML(false).div("grid") {
            for (author in authors) {
                val fullName = author.id.fullName
                val encodedFullName = URLEncoder.encode(fullName, "UTF-8")
                NavTile(
                    fullName,
                    "View books by this author",
                    "/api/author/view/$encodedFullName"
                )
            }
        }

        val breadcrumbs = buildAuthorBreadcrumbs(prefix)
        return ok(smartHtml(req, x, breadcrumbs))
    }

    suspend fun authorView(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val encodedFullName = withContext(Dispatchers.IO) {
            URLEncoder.encode(fullName, "UTF-8")
        }

        val x = createHTML(false).div("grid") {
            NavTile(
                "By Series",
                "View series by this author",
                "/api/author/view/$encodedFullName/series"
            )
            NavTile(
                "Without Series",
                "View books without series",
                "/api/author/view/$encodedFullName/noseries"
            )
            NavTile(
                "All Books",
                "View all books by this author",
                "/api/author/view/$encodedFullName/all"
            )
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$encodedFullName"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    suspend fun authorSeries(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val encodedFullName = URLEncoder.encode(fullName, "UTF-8")

        val series = bookService.findSeriesByAuthorFullName(fullName).toList()

        val x = createHTML(false).div("grid") {
            for (seriesResult in series) {
                val encodedSeries = URLEncoder.encode(seriesResult.id, "UTF-8")
                NavTile(
                    seriesResult.id,
                    "View books in this series",
                    "/api/author/view/$encodedFullName/series/$encodedSeries"
                )
            }
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$encodedFullName",
                "Series" to "/api/author/view/$encodedFullName/series"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
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
        val encodedSeries = withContext(Dispatchers.IO) {
            URLEncoder.encode(seriesName, "UTF-8")
        }

        val sort = Sort.by(Sort.Direction.ASC, "sequenceNumber")
        val books = bookService.findBooksBySeries(seriesName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val x = createHTML(false).div("grid") {
            for (book in books) {
                BookTile(book, imageTypes, shortDescriptions)
            }
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$encodedFullName",
                "Series" to "/api/author/view/$encodedFullName/series",
                seriesName to "/api/author/view/$encodedFullName/series/$encodedSeries"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    suspend fun authorNoSeriesBooks(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val encodedFullName = URLEncoder.encode(fullName, "UTF-8")

        val sort = Sort.by(Sort.Direction.ASC, "name")
        val books = bookService.findBooksByAuthorWithoutSeriesFullName(fullName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val x = createHTML(false).div("grid") {
            for (book in books) {
                BookTile(book, imageTypes, shortDescriptions)
            }
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$encodedFullName",
                "Without Series" to "/api/author/view/$encodedFullName/noseries"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    suspend fun authorAllBooks(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val encodedFullName = req.pathVariable("fullName")

        val sort = Sort.by(Sort.Direction.ASC, "name")
        val books = bookService.findBooksByAuthorFullName(fullName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val x = createHTML(false).div("grid") {
            for (book in books) {
                BookTile(book, imageTypes, shortDescriptions)
            }
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$encodedFullName",
                "All Books" to "/api/author/view/$encodedFullName/all"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    private fun buildAuthorBreadcrumbs(prefix: String): String {
        val breadcrumbItems = mutableListOf<Pair<String, String>>()
        breadcrumbItems.add("Library" to "/api")
        breadcrumbItems.add("Authors" to "/api/author")

        // Add breadcrumbs for each level of the prefix
        for (i in 1..prefix.length) {
            val currentPrefix = prefix.substring(0, i)
            breadcrumbItems.add(currentPrefix to "/api/author/$currentPrefix")
        }

        return BreadCrumbs(breadcrumbItems)
    }

    suspend fun downloadBook(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val targetFormat = req.pathVariableOrNull("format")

        return bookService.downloadBook(bookId, targetFormat)
    }

    suspend fun getBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        return bookService.getBookCover(bookId, seaweedFSService)
    }

    suspend fun getBookInfo(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")

        // Get the book from the database
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // Get the book annotation from the original file
        val commonBook = bookService.obtainBook(book.path)
        val annotation = commonBook?.annotation

        // Create HTML for the modal content
        val html = createHTML().div("box") {
            div("content") {
                // Show full-size cover if available
                if (book.hasCover) {
                    figure("image") {
                        img(src = "/opds/fullimage/${book.id}") {
                            attributes["loading"] = "lazy"
                            attributes["alt"] = "Book cover for ${book.name}"
                        }
                    }
                }

                h3 { +book.name }

                if (book.authors.isNotEmpty()) {
                    div("tags") {
                        strong { +"Author(s): " }
                        book.authors.forEach { author ->
                            a(href = "/api/author/view/${URLEncoder.encode(author.fullName, Charset.defaultCharset())}", classes = "tag is-info") {
                                +"${author.firstName} ${author.lastName}"
                            }
                        }
                    }
                }

                if (book.sequence != null) {
                    p {
                        strong { +"Series: " }
                        a(href = "/api/series/${URLEncoder.encode(book.sequence, Charset.defaultCharset())}") {
                            +book.sequence
                        }
                        if (book.sequenceNumber != null) {
                            +" (#${book.sequenceNumber})"
                        }
                    }
                }

                if (book.genres.isNotEmpty()) {
                    div("tags") {
                        strong { +"Genres: " }
                        book.genres.forEach { genre ->
                            span("tag") {
                                +genre
                            }
                        }
                    }
                }

                p {
                    strong { +"File size: " }
                    +"${(book.size / 1024)} KB"
                }

                // Show book description if available
                if (!annotation.isNullOrEmpty()) {
                    div {
                        strong { +"Description: " }
                        div("book-description") {
                            +annotation
                        }
                    }
                }
            }
        }

        return ok()
            .contentType(MediaType.TEXT_HTML)
            .bodyValueAndAwait(html)
    }

    suspend fun seriesBooks(req: ServerRequest): ServerResponse {
        val seriesName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("series"), "UTF-8")
        }

        val sort = Sort.by(Sort.Direction.ASC, "sequenceNumber")
        val books = bookService.findBooksBySeries(seriesName, sort).toList()

        val imageTypes = bookService.imageTypes(books)
        val shortDescriptions = bookService.shortDescriptions(books)

        val x = createHTML(false).div("grid") {
            for (book in books) {
                BookTile(book, imageTypes, shortDescriptions)
            }
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Series" to "/api/series",
                seriesName to "/api/series/${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(
                            seriesName,
                            "UTF-8"
                        )
                    }
                }"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    suspend fun getFullBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val isHtmxRequest = req.headers().firstHeader("HX-Request") == "true"

        if (isHtmxRequest) {
            val html = createHTML().div("box") {
                figure("image") {
                    img(src = "/opds/fullimage/${bookId}") {
                        attributes["loading"] = "lazy"
                        attributes["alt"] = "Full-size book cover"
                    }
                }
            }

            return ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValueAndAwait(html)
        } else {
            return bookService.getFullBookCover(bookId, seaweedFSService)
        }
    }
}
