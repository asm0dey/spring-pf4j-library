package com.github.asm0dey.opdsko_spring.handler

import com.github.asm0dey.opdsko.common.FormatConverter
import com.github.asm0dey.opdsko_spring.model.BookTileViewModel
import com.github.asm0dey.opdsko_spring.model.BreadcrumbsViewModel
import com.github.asm0dey.opdsko_spring.model.NavTileViewModel
import com.github.asm0dey.opdsko_spring.renderer.OpdsViewRenderer
import com.github.asm0dey.opdsko_spring.service.BookService
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class OpdsHandler(
    bookService: BookService,
    formatConverters: List<FormatConverter>,
    private val viewRenderer: OpdsViewRenderer
) : AbstractHandler(bookService, formatConverters) {

    private val OPDS_MIME_TYPE = MediaType.parseMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")

    override fun NavTile(model: NavTileViewModel): String {
        return viewRenderer.NavTile(model.title, model.subtitle, model.href)
    }

    override fun BookTile(model: BookTileViewModel, additionalFormats: List<String>): String {
        val bookId = model.book.id ?: ""
        return viewRenderer.BookTile(
            model.book, mapOf(bookId to model.coverUrl), mapOf(bookId to model.description), additionalFormats
        )
    }

    override fun Breadcrumbs(model: BreadcrumbsViewModel): String {
        return viewRenderer.Breadcrumbs(model.items)
    }

    override fun fullPage(content: String, breadcrumbs: String, req: ServerRequest, isAdmin: Boolean): String {
        return viewRenderer.fullPage(content, breadcrumbs, pagination = "", fullRender = true, isAdmin = isAdmin)
    }

    override fun fullPage(
        content: String,
        breadcrumbs: String,
        pagination: String,
        req: ServerRequest,
        isAdmin: Boolean
    ): String {
        return viewRenderer.fullPage(content, breadcrumbs, pagination, fullRender = true, isAdmin = isAdmin)
    }


    override fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String {
        return viewRenderer.Pagination(currentPage, totalPages, baseUrl)
    }

    override fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String {
        return viewRenderer.IndeterminatePagination(currentPage, hasMoreItems, baseUrl)
    }

    override val contentType: MediaType = OPDS_MIME_TYPE

    override val baseUrl: String = "/opds"
}
