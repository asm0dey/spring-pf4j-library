package com.github.asm0dey.opdsko_spring.handler

import com.github.asm0dey.opdsko.common.FormatConverter
import com.github.asm0dey.opdsko_spring.BookService
import com.github.asm0dey.opdsko_spring.model.BookTileViewModel
import com.github.asm0dey.opdsko_spring.model.BreadcrumbsViewModel
import com.github.asm0dey.opdsko_spring.model.NavTileViewModel
import com.github.asm0dey.opdsko_spring.renderer.SimpleViewRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URLEncoder.encode

@Component
class SimpleHandler(
    val bookService: BookService,
    formatConverters: List<FormatConverter>,
    private val viewRenderer: SimpleViewRenderer
) : AbstractHandler(bookService, formatConverters) {

    override fun NavTile(model: NavTileViewModel): String {
        return viewRenderer.NavTile(model.title, model.subtitle, model.href)
    }

    override fun BookTile(model: BookTileViewModel, additionalFormats: List<String>): String {
        val bookId = model.book.id ?: ""
        return viewRenderer.BookTile(
            model.book, mapOf(bookId to model.coverUrl), mapOf(bookId to model.description), additionalFormats)
    }

    override fun Breadcrumbs(model: BreadcrumbsViewModel): String {
        return viewRenderer.Breadcrumbs(model.items)
    }

    override fun fullPage(content: String, breadcrumbs: String, req: ServerRequest): String {
        return viewRenderer.fullPage(content, breadcrumbs)
    }

    override fun fullPage(content: String, breadcrumbs: String, pagination: String, req: ServerRequest): String {
        return viewRenderer.fullPage(content, breadcrumbs, pagination)
    }

    override fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String {
        return viewRenderer.Pagination(currentPage, totalPages, baseUrl)
    }

    override fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String {
        return viewRenderer.IndeterminatePagination(currentPage, hasMoreItems, baseUrl)
    }

    override val contentType: MediaType = MediaType.TEXT_HTML

    override val baseUrl: String = "/simple"

    /**
     * Handler for book information
     */
    suspend fun getBookInfo(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // Get short descriptions for this book
        val descriptions = getBookDescriptions(listOf(book))
        val description = descriptions[bookId]

        // Get additional formats
        val additionalFormats = additionalFormats(book.path.substringAfterLast('.'))

        // Generate content using the renderer
        val content = viewRenderer.bookInfo(book, description, additionalFormats, baseUrl)

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    book.name to "${baseUrl}/book/$bookId/info"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(content, breadcrumbs, req))
    }
    /**
     * Handler for full-size book cover
     */
    suspend fun getFullBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val book = bookService.getBookById(bookId) ?: return ServerResponse.notFound().buildAndAwait()

        // Generate content using the renderer
        val content = viewRenderer.fullBookCover(book, baseUrl)

        val breadcrumbs = Breadcrumbs(
            BreadcrumbsViewModel(
                listOf(
                    "Library" to baseUrl,
                    book.name to "${baseUrl}/book/$bookId/info",
                    "Full Cover" to "${baseUrl}/book/$bookId/fullimage"
                )
            )
        )

        return ok().bodyValueAndAwait(fullPage(content, breadcrumbs, req))
    }

}
