package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.FormatConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Sort
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
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
            NavTile("New books", "Recent publications from this catalog", "/api/new/1")
            NavTile("Books by series", "Authors by first letters", "/api/series/browse")
            NavTile("Books by author", "Authors by first letters", "/api/author")
            NavTile("Genres", "Books by genres", "/api/genre")
        }
        return ok(smartHtml(req, x, BreadCrumbs("Library" to "/api")))
    }

    suspend fun search(req: ServerRequest): ServerResponse {
        val page = req.queryParamOrNull("page")?.toIntOrNull()?.minus(1) ?: 0
        val searchTerm = req.queryParam("search").getOrNull()!!
        val searchBookByText = bookService.searchBookByText(searchTerm, page)
        println(searchBookByText)
        val x = createHTML(false).div("grid") {
            NavTile("New books", "Recent publications from this catalog", "/api/new/1")
            NavTile("Books by series", "Authors by first letters", "/api/series/browse")
            NavTile("Books by author", "Authors by first letters", "/api/author")
            NavTile("Genres", "Books by genres", "/api/genre")
        }
        return ok(smartHtml(req, x, BreadCrumbs("Library" to "/api")))
    }

    suspend fun new(req: ServerRequest): ServerResponse {
        val page = req.pathVariableOrNull("page")?.toIntOrNull()?.minus(1) ?: 0
        val books = bookService.newBooks(page)
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
        bookWithInfo: BookWithInfo,
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
                    "/api/author/${prefixResult.id}"
                )
            }
        }

        val breadcrumbs = buildAuthorBreadcrumbs(prefix)
        return ok(smartHtml(req, x, breadcrumbs))
    }

    /**
     * Handler for author navigation - full author names
     */
    suspend fun authorFullNames(req: ServerRequest): ServerResponse {
        val prefix = req.pathVariable("prefix")
        val authors = bookService.findAuthorsByPrefix(prefix).toList()

        val x = createHTML(false).div("grid") {
            for (author in authors) {
                val fullName = "${author.id.lastName}, ${author.id.firstName}"
                val encodedLastName = URLEncoder.encode(author.id.lastName, "UTF-8")
                val encodedFirstName = URLEncoder.encode(author.id.firstName, "UTF-8")
                NavTile(
                    fullName,
                    "View books by this author",
                    "/api/author/view/$encodedLastName/$encodedFirstName"
                )
            }
        }

        val breadcrumbs = buildAuthorBreadcrumbs(prefix)
        return ok(smartHtml(req, x, breadcrumbs))
    }

    /**
     * Handler for author view - shows navigation options for an author
     */
    suspend fun authorView(req: ServerRequest): ServerResponse {
        val lastName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("lastName"), "UTF-8")
        }
        val firstName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("firstName"), "UTF-8")
        }
        val fullName = "$lastName, $firstName"

        val x = createHTML(false).div("grid") {
            NavTile(
                "By Series",
                "View series by this author",
                "/api/author/view/$lastName/$firstName/series"
            )
            NavTile(
                "Without Series",
                "View books without series",
                "/api/author/view/$lastName/$firstName/noseries"
            )
            NavTile(
                "All Books",
                "View all books by this author",
                "/api/author/view/$lastName/$firstName/all"
            )
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$lastName/$firstName"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    /**
     * Handler for author's series
     */
    suspend fun authorSeries(req: ServerRequest): ServerResponse {
        val lastName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("lastName"), "UTF-8")
        }
        val firstName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("firstName"), "UTF-8")
        }
        val fullName = "$lastName, $firstName"

        val series = bookService.findSeriesByAuthor(lastName, firstName).toList()

        val x = createHTML(false).div("grid") {
            for (seriesResult in series) {
                val encodedSeries = URLEncoder.encode(seriesResult.id, "UTF-8")
                NavTile(
                    seriesResult.id,
                    "View books in this series",
                    "/api/author/view/$lastName/$firstName/series/$encodedSeries"
                )
            }
        }

        val breadcrumbs = BreadCrumbs(
            listOf(
                "Library" to "/api",
                "Authors" to "/api/author",
                fullName to "/api/author/view/$lastName/$firstName",
                "Series" to "/api/author/view/$lastName/$firstName/series"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    /**
     * Handler for books in a series by an author
     */
    suspend fun authorSeriesBooks(req: ServerRequest): ServerResponse {
        val lastName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("lastName"), "UTF-8")
        }
        val firstName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("firstName"), "UTF-8")
        }
        val seriesName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("series"), "UTF-8")
        }
        val fullName = "$lastName, $firstName"

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
                fullName to "/api/author/view/$lastName/$firstName",
                "Series" to "/api/author/view/$lastName/$firstName/series",
                seriesName to "/api/author/view/$lastName/$firstName/series/${
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

    /**
     * Handler for books without series by an author
     */
    suspend fun authorNoSeriesBooks(req: ServerRequest): ServerResponse {
        val lastName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("lastName"), "UTF-8")
        }
        val firstName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("firstName"), "UTF-8")
        }
        val fullName = "$lastName, $firstName"

        val sort = Sort.by(Sort.Direction.ASC, "name")
        val books = bookService.findBooksByAuthorWithoutSeries(lastName, firstName, sort).toList()

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
                fullName to "/api/author/view/$lastName/$firstName",
                "Without Series" to "/api/author/view/$lastName/$firstName/noseries"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    /**
     * Handler for all books by an author
     */
    suspend fun authorAllBooks(req: ServerRequest): ServerResponse {
        val lastName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("lastName"), "UTF-8")
        }
        val firstName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("firstName"), "UTF-8")
        }
        val fullName = "$lastName, $firstName"

        val sort = Sort.by(Sort.Direction.ASC, "name")
        val books = bookService.findBooksByAuthor(lastName, firstName, sort).toList()

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
                fullName to "/api/author/view/$lastName/$firstName",
                "All Books" to "/api/author/view/$lastName/$firstName/all"
            )
        )

        return ok(smartHtml(req, x, breadcrumbs))
    }

    /**
     * Helper function to build breadcrumbs for author navigation
     */
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

        // Get the book from the repository
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // Determine the original extension
        val originalExtension = book.path.substringAfterLast('.')

        if (targetFormat != null) {
            // Handle format conversion
            val convertedStream = bookService.convertBook(book.path, targetFormat)
                ?: return ServerResponse.badRequest().bodyValueAndAwait("Conversion to $targetFormat not supported")

            val fileName = generateFileName(book, targetFormat)
            val contentType = getContentTypeForExtension(targetFormat)

            val headers = HttpHeaders().apply {
                contentDisposition = ContentDisposition.builder("attachment")
                    .filename(fileName)
                    .build()
            }
            convertedStream.use {
                return ServerResponse.ok()
                    .headers { it.addAll(headers) }
                    .contentType(MediaType.parseMediaType(contentType))
                    .bodyValueAndAwait(InputStreamResource(it))
            }

        }
        // Handle direct download
        val fileName = generateFileName(book, originalExtension)
        val contentType = getContentTypeForExtension(originalExtension)

        val headers = HttpHeaders().apply {
            contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName)
                .build()
        }

        return ServerResponse.ok()
            .headers { it.addAll(headers) }
            .contentType(MediaType.parseMediaType(contentType))
            .bodyValueAndAwait(InputStreamResource(bookService.getBookData(book.path)))
    }

    /**
     * Generates a file name from the book's metadata.
     * The format is: "BookName [SequenceName #SequenceNumber].extension"
     * If sequence name or number is not available, they are omitted.
     */
    private fun generateFileName(book: Book, extension: String): String {
        val baseFileName = buildString {
            append(book.name.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

            if (book.sequence != null) {
                append(" [")
                append(book.sequence.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

                if (book.sequenceNumber != null) {
                    append(" #")
                    append(book.sequenceNumber)
                }

                append("]")
            }
        }

        return "$baseFileName.$extension"
    }

    private fun getContentTypeForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "epub" -> "application/epub+zip"
            "fb2" -> "application/x-fictionbook+xml"
            "pdf" -> "application/pdf"
            "mobi" -> "application/x-mobipocket-ebook"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    /**
     * Serves a book cover preview image from SeaweedFS or retrieves it from the book on demand.
     * If the cover is not in SeaweedFS, it retrieves it from the book and caches it to SeaweedFS.
     * If the book doesn't have a cover, it returns a 404 Not Found response.
     * 
     * @param req The server request
     * @return The server response with the preview image data
     */
    suspend fun getBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")

        // Get the book from the database first to check if it has a cover
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // If the book doesn't have a cover, return 404 Not Found immediately
        if (!book.hasCover) {
            return ServerResponse.notFound().buildAndAwait()
        }

        // Try to get the book cover preview from SeaweedFS first
        var coverInputStream = seaweedFSService.getBookCoverPreview(bookId)
        val contentType: String

        if (coverInputStream == null) {
            // Try to get the original cover (the preview might not exist yet)
            coverInputStream = seaweedFSService.getBookCover(bookId)

            if (coverInputStream == null) {
                // If we can't find the cover in SeaweedFS, let's try to get it from the book itself
                // Cover not found in SeaweedFS, retrieve it from the book
                val commonBook = bookService.obtainBook(book.path) ?: return ServerResponse.notFound().buildAndAwait()

                // Check if the book has a cover
                if (commonBook.cover == null || commonBook.coverContentType == null) {
                    // Update the book in the database to reflect that it doesn't have a cover
                    // This is a case where the hasCover flag was true but the book actually doesn't have a cover
                    val updatedBook = book.copy(hasCover = false)
                    bookService.saveBook(updatedBook)
                    return ServerResponse.notFound().buildAndAwait()
                }

                // Cache the cover to SeaweedFS (this will also create and cache the preview)
                seaweedFSService.saveBookCover(bookId, commonBook.cover!!, commonBook.coverContentType!!)

                // Get the newly cached cover preview from SeaweedFS
                coverInputStream = seaweedFSService.getBookCoverPreview(bookId)
                    ?: return ServerResponse.notFound().buildAndAwait()

                contentType = commonBook.coverContentType!!
            } else {
                // We found the original cover but not the preview, so we'll use the original
                // Get the content type of the cover image from SeaweedFS
                contentType = seaweedFSService.getBookCoverContentType(bookId)
            }
        } else {
            // Get the content type of the cover preview image from SeaweedFS
            contentType = seaweedFSService.getBookCoverPreviewContentType(bookId)
        }

        // Create an InputStreamResource with the cover image data
        val inputStreamResource = InputStreamResource(coverInputStream)

        return ServerResponse.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .bodyValueAndAwait(inputStreamResource)
    }

    /**
     * Serves book information in a modal.
     * This is used when a user clicks the "Info" link on a book card.
     * 
     * @param req The server request
     * @return The server response with HTML for the modal
     */
    suspend fun getBookInfo(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")

        // Get the book from the database
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // Create HTML for the modal content
        val html = createHTML().div("box") {
            div("content") {
                h3 { +book.name }

                if (book.authors.isNotEmpty()) {
                    p {
                        strong { +"Author(s): " }
                        +book.authors.joinToString(", ") { "${it.firstName} ${it.lastName}" }
                    }
                }

                if (book.sequence != null) {
                    p {
                        strong { +"Series: " }
                        +book.sequence!!
                        if (book.sequenceNumber != null) {
                            +" (#${book.sequenceNumber})"
                        }
                    }
                }

                if (book.genres.isNotEmpty()) {
                    p {
                        strong { +"Genres: " }
                        +book.genres.joinToString(", ")
                    }
                }

                p {
                    strong { +"File size: " }
                    +"${(book.size / 1024).toString()} KB"
                }

                p {
                    strong { +"File path: " }
                    +book.path
                }

                // Add a download link
                div("buttons") {
                    a("/opds/book/${book.id}/download", classes = "button is-primary") {
                        +"Download"
                    }
                }
            }
        }

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .bodyValueAndAwait(html)
    }

    /**
     * Serves a full-size book cover image from SeaweedFS.
     * This is used when a user clicks on the preview image to see the full-size image in a popup.
     * If the book doesn't have a cover, it returns a 404 Not Found response.
     * 
     * @param req The server request
     * @return The server response with the full-size image data or HTML for the modal
     */
    suspend fun getFullBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")

        // Get the book from the database first to check if it has a cover
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // If the book doesn't have a cover, return 404 Not Found immediately
        if (!book.hasCover) {
            return ServerResponse.notFound().buildAndAwait()
        }

        // Get the full-size cover image from SeaweedFS
        val coverInputStream = seaweedFSService.getBookCover(bookId)

        // If the cover is not found in SeaweedFS, update the hasCover flag to false and return 404
        if (coverInputStream == null) {
            // Update the book in the database to reflect that it doesn't have a cover
            val updatedBook = book.copy(hasCover = false)
            bookService.saveBook(updatedBook)
            return ServerResponse.notFound().buildAndAwait()
        }

        // Get the content type of the cover image
        val contentType = seaweedFSService.getBookCoverContentType(bookId)

        // Create an InputStreamResource with the cover image data
        val inputStreamResource = InputStreamResource(coverInputStream)

        // Check if this is an HTMX request for the modal content
        val isHtmxRequest = req.headers().firstHeader("HX-Request") == "true"

        if (isHtmxRequest) {
            // Return HTML for the modal content with the full-size image
            val html = createHTML().div("box") {
                figure("image") {
                    img(src = "/opds/fullimage/${bookId}") {
                        attributes["loading"] = "lazy"
                        attributes["alt"] = "Full-size book cover"
                    }
                }
            }

            return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValueAndAwait(html)
        } else {
            // Return the image data directly
            return ServerResponse.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .bodyValueAndAwait(inputStreamResource)
        }
    }
}
