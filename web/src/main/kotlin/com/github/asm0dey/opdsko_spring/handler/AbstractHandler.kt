package com.github.asm0dey.opdsko_spring.handler

import com.github.asm0dey.opdsko.common.FormatConverter
import com.github.asm0dey.opdsko_spring.Book
import com.github.asm0dey.opdsko_spring.model.BookTileViewModel
import com.github.asm0dey.opdsko_spring.model.BreadcrumbsViewModel
import com.github.asm0dey.opdsko_spring.model.NavTileViewModel
import com.github.asm0dey.opdsko_spring.service.BookService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.queryParamOrNull
import java.net.URLDecoder
import java.net.URLEncoder.encode
import kotlin.jvm.optionals.getOrNull

@Suppress("FunctionName")
abstract class AbstractHandler(
    private val bookService: BookService,
    private val formatConverters: List<FormatConverter>
) {
    private val logger = LoggerFactory.getLogger(AbstractHandler::class.java)

    // Abstract methods that must be implemented by subclasses
    protected abstract fun NavTile(model: NavTileViewModel): String
    protected abstract fun BookTile(model: BookTileViewModel, additionalFormats: List<String>): String
    protected abstract fun Breadcrumbs(model: BreadcrumbsViewModel): String
    protected abstract fun fullPage(content: String, breadcrumbs: String, req: ServerRequest): String
    protected abstract fun fullPage(
        content: String,
        breadcrumbs: String,
        pagination: String,
        req: ServerRequest
    ): String

    protected abstract fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String
    protected abstract fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String
    abstract val contentType: MediaType
    protected abstract val baseUrl: String
    protected open suspend fun ok() = ServerResponse.ok().contentType(contentType)

    // Common handler methods
    suspend fun homePage(req: ServerRequest): ServerResponse {
        val navTiles = listOf(
            NavTileViewModel("New books", "Recent publications from this catalog", "${baseUrl}/new"),
            NavTileViewModel("Books by series", "Authors by first letters", "${baseUrl}/series/browse"),
            NavTileViewModel("Books by author", "Authors by first letters", "${baseUrl}/author"),
            NavTileViewModel("Genres", "Books by genres", "${baseUrl}/genre")
        )

        val content = navTiles.joinToString("") { NavTile(it) }
        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf("Library" to baseUrl)
            )
        )

        return ok().bodyValueAndAwait(fullPage(content, breadcrumbs, req))
    }

    suspend fun search(req: ServerRequest): ServerResponse {
        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val searchTerm = req.queryParam("search").getOrNull()!!
        val books = bookService.searchBookByName(searchTerm, page)

        val images = getBookImages(books)
        val descriptions = getBookDescriptions(books)

        val bookTiles = books.joinToString("") { book ->
            val model = BookTileViewModel(book, images[book.id], descriptions[book.id])
            BookTile(
                BookTileViewModel(book, images[book.id], descriptions[book.id]), additionalFormats(model.book.path)
            )
        }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Search: $searchTerm" to "${baseUrl}/search?search=$searchTerm"
                )
            )
        )

        // Assume there are more items if we have a full page of results
        val hasMoreItems = books.size == 15
        val pagination = IndeterminatePagination(pageParam, hasMoreItems, "${baseUrl}/search?search=$searchTerm")

        return ok().bodyValueAndAwait(fullPage(bookTiles, breadcrumbs, pagination, req))
    }

    suspend fun new(req: ServerRequest): ServerResponse {
        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val pagedBooks = bookService.newBooks(page)
        val books = pagedBooks.books

        val images = getBookImages(books)
        val descriptions = getBookDescriptions(books)

        val bookTiles = books.joinToString("") { book ->
            val model = BookTileViewModel(book, images[book.id], descriptions[book.id])
            BookTile(
                BookTileViewModel(book, images[book.id], descriptions[book.id]), additionalFormats(model.book.path)
            )
        }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "New" to "${baseUrl}/new"
                )
            )
        )

        // Assume there are more items if we have a full page of results
        val hasMoreItems = books.size >= 24
        val pagination = IndeterminatePagination(pageParam, hasMoreItems, "${baseUrl}/new")

        return ok().bodyValueAndAwait(fullPage(bookTiles, breadcrumbs, pagination, req))
    }

    /**
     * Handler for author navigation - first level (first letters)
     */
    suspend fun authorFirstLevel(req: ServerRequest): ServerResponse {
        val letters = bookService.findAuthorFirstLetters().toList()
        val navTiles = letters.joinToString("") { letter ->
            val title = if (letter.count > 1) letter.id else letter.id
            NavTile(
                NavTileViewModel(
                    title,
                    "Authors starting with ${letter.id} (${letter.count})",
                    "${baseUrl}/author/${letter.id}"
                )
            )
        }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Authors" to "${baseUrl}/author"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(navTiles, breadcrumbs, req))
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

        val navTiles = prefixes.joinToString("") { prefixResult ->
            val title = if (prefixResult.count > 1) "${prefixResult.id} (${prefixResult.count})" else prefixResult.id
            NavTile(
                NavTileViewModel(
                    title,
                    "Authors starting with ${prefixResult.id}",
                    "${baseUrl}/author/${encode(prefixResult.id, "UTF-8")}"
                )
            )
        }

        val breadcrumbs = Breadcrumbs(buildAuthorBreadcrumbs(prefix))
        return ok().bodyValueAndAwait(fullPage(navTiles, breadcrumbs, req))
    }

    /**
     * Handler for author navigation - full names
     */
    suspend fun authorFullNames(req: ServerRequest): ServerResponse {
        val prefix = req.pathVariable("prefix")
        val authors = bookService.findAuthorsByPrefix(prefix).toList()

        val navTiles = authors.joinToString("") { author ->
            val fullName = author.id.fullName
            val encodedFullName = encode(fullName, "UTF-8")
            NavTile(
                NavTileViewModel(
                    fullName,
                    "View books by this author",
                    "${baseUrl}/author/view/$encodedFullName"
                )
            )
        }

        val breadcrumbs = Breadcrumbs(buildAuthorBreadcrumbs(prefix))
        return ok().bodyValueAndAwait(fullPage(navTiles, breadcrumbs, req))
    }

    /**
     * Handler for author view
     */
    suspend fun authorView(req: ServerRequest): ServerResponse {
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("fullName"), "UTF-8")
        }
        val encodedFullName = req.pathVariable("fullName")

        // Check if the author has any series
        val seriesExist = bookService.findSeriesByAuthorFullName(fullName).firstOrNull() != null

        // If the author doesn't have any series, redirect directly to the "All Books" view
        if (!seriesExist) return authorAllBooks(req)

        val navTiles = listOf(
            NavTileViewModel(
                "By Series",
                "View series by this author",
                "${baseUrl}/author/view/$encodedFullName/series"
            ),
            NavTileViewModel(
                "Without Series",
                "View books without series",
                "${baseUrl}/author/view/$encodedFullName/noseries"
            ),
            NavTileViewModel(
                "All Books",
                "View all books by this author",
                "${baseUrl}/author/view/$encodedFullName/all"
            )
        ).joinToString("") { NavTile(it) }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Authors" to "${baseUrl}/author",
                    fullName to "${baseUrl}/author/view/$encodedFullName"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(navTiles, breadcrumbs, req))
    }

    /**
     * Handler for author series
     */
    suspend fun authorSeries(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }

        val series = bookService.findSeriesByAuthorFullName(fullName).toList()

        val navTiles = series.joinToString("") { seriesResult ->
            val encodedSeries = encode(seriesResult.id, "UTF-8")
            NavTile(
                NavTileViewModel(
                    seriesResult.id,
                    "View books in this series",
                    "${baseUrl}/author/view/$encodedFullName/series/$encodedSeries"
                )
            )
        }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Authors" to "${baseUrl}/author",
                    fullName to "${baseUrl}/author/view/$encodedFullName",
                    "Series" to "${baseUrl}/author/view/$encodedFullName/series"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(navTiles, breadcrumbs, req))
    }

    /**
     * Handler for author series books
     */
    suspend fun authorSeriesBooks(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val encodedSeries = req.pathVariable("series")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }
        val seriesName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedSeries, "UTF-8")
        }
        val bookTiles = generateBookTilesForSeriesAndAuthor(seriesName, fullName)

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Authors" to "${baseUrl}/author",
                    fullName to "${baseUrl}/author/view/$encodedFullName",
                    "Series" to "${baseUrl}/author/view/$encodedFullName/series",
                    seriesName to "${baseUrl}/author/view/$encodedFullName/series/$encodedSeries"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(bookTiles, breadcrumbs, req))
    }

    /**
     * Handler for author books without series
     */
    suspend fun authorNoSeriesBooks(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }

        val sort = Sort.by(Sort.Direction.ASC, "name")
        val books = bookService.findBooksByAuthorWithoutSeriesFullName(fullName, sort).toList()

        val images = getBookImages(books)
        val descriptions = getBookDescriptions(books)

        val bookTiles = books.joinToString("") { book ->
            val model = BookTileViewModel(book, images[book.id], descriptions[book.id])
            BookTile(
                BookTileViewModel(book, images[book.id], descriptions[book.id]), additionalFormats(model.book.path)
            )
        }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Authors" to "${baseUrl}/author",
                    fullName to "${baseUrl}/author/view/$encodedFullName",
                    "Without Series" to "${baseUrl}/author/view/$encodedFullName/noseries"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(bookTiles, breadcrumbs, req))
    }

    /**
     * Handler for all author books
     */
    suspend fun authorAllBooks(req: ServerRequest): ServerResponse {
        val encodedFullName = req.pathVariable("fullName")
        val fullName = withContext(Dispatchers.IO) {
            URLDecoder.decode(encodedFullName, "UTF-8")
        }

        val pageParam = req.queryParamOrNull("page")?.toIntOrNull() ?: 1
        val page = pageParam - 1 // Convert to 0-based for repository
        val pagedBooks = bookService.findBooksByAuthorFullName(fullName, page)
        val books = pagedBooks.books

        val images = getBookImages(books)
        val descriptions = getBookDescriptions(books)

        val bookTiles = books.joinToString("") { book ->
            val model = BookTileViewModel(book, images[book.id], descriptions[book.id])
            BookTile(
                BookTileViewModel(book, images[book.id], descriptions[book.id]), additionalFormats(model.book.path)
            )
        }

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Authors" to "${baseUrl}/author",
                    fullName to "${baseUrl}/author/view/$encodedFullName",
                    "All Books" to "${baseUrl}/author/view/$encodedFullName/all"
                )
            )
        )

        val pagination = Pagination(pageParam, pagedBooks.total.toInt(), "${baseUrl}/author/view/$encodedFullName/all")

        return ok().bodyValueAndAwait(fullPage(bookTiles, breadcrumbs, pagination, req))
    }

    fun additionalFormats(path: String) = formatConverters
        .filter { it.canConvert(path.substringAfterLast('.')) }
        .map { it.targetFormat }

    /**
     * Handler for series books
     */
    suspend fun seriesBooks(req: ServerRequest): ServerResponse {
        val seriesName = withContext(Dispatchers.IO) {
            URLDecoder.decode(req.pathVariable("series"), "UTF-8")
        }
        val encodedSeries = withContext(Dispatchers.IO) {
            encode(seriesName, "UTF-8")
        }

        val bookTiles = generateBookTilesForSeries(seriesName)

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    "Series" to "${baseUrl}/series",
                    seriesName to "${baseUrl}/series/$encodedSeries"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(bookTiles, breadcrumbs, req))
    }

    private suspend fun AbstractHandler.generateBookTilesForSeries(seriesName: String): String {
        val sort = Sort.by(Sort.Direction.ASC, "sequenceNumber")
        val books = bookService.findBooksBySeries(seriesName, sort).toList()

        val images = getBookImages(books)
        val descriptions = getBookDescriptions(books)

        return books.joinToString("") { book ->
            val model = BookTileViewModel(book, images[book.id], descriptions[book.id])
            BookTile(
                BookTileViewModel(book, images[book.id], descriptions[book.id]), additionalFormats(model.book.path)
            )
        }
    }

    private suspend fun AbstractHandler.generateBookTilesForSeriesAndAuthor(
        seriesName: String,
        authorFullName: String
    ): String {
        val sort = Sort.by(Sort.Direction.ASC, "sequenceNumber")
        val books = bookService.findBooksBySeriesAndAuthorFullName(seriesName, authorFullName, sort).toList()

        val images = getBookImages(books)
        val descriptions = getBookDescriptions(books)

        return books.joinToString("") { book ->
            BookTile(
                BookTileViewModel(book, images[book.id], descriptions[book.id]), additionalFormats(book.path)
            )
        }
    }

    // Helper methods
    private fun buildAuthorBreadcrumbs(prefix: String): BreadcrumbsViewModel {
        val breadcrumbItems = mutableListOf<Pair<String, String>>()
        breadcrumbItems.add("Library" to baseUrl)
        breadcrumbItems.add("Authors" to "${baseUrl}/author")

        // Add breadcrumbs for each level of the prefix
        for (i in 1..prefix.length) {
            val currentPrefix = prefix.substring(0, i)
            breadcrumbItems.add(currentPrefix to "${baseUrl}/author/$currentPrefix")
        }

        return BreadcrumbsViewModel(breadcrumbItems)
    }

    private fun getBookImages(books: List<Book>): Map<String, String?> {
        return bookService.imageTypes(books)
    }

    protected suspend fun getBookDescriptions(books: List<Book>): Map<String, String?> {
        return bookService.shortDescriptions(books)
    }
}
