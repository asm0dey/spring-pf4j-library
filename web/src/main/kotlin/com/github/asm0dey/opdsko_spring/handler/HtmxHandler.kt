package com.github.asm0dey.opdsko_spring.handler

import com.github.asm0dey.opdsko.common.FormatConverter
import com.github.asm0dey.opdsko_spring.service.BookService
import com.github.asm0dey.opdsko_spring.model.BookTileViewModel
import com.github.asm0dey.opdsko_spring.model.BreadcrumbsViewModel
import com.github.asm0dey.opdsko_spring.model.NavTileViewModel
import com.github.asm0dey.opdsko_spring.renderer.HtmxViewRenderer
import org.springframework.context.annotation.DependsOn
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@DependsOn("springPluginManager")
@Component
class HtmxHandler(
    bookService: BookService,
    private val viewRenderer: HtmxViewRenderer,
    formatConverters: List<FormatConverter>
) : AbstractHandler(bookService, formatConverters, true) {

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
        return viewRenderer.fullPage(
            content,
            breadcrumbs,
            pagination = "",
            fullRender = !req.headers().firstHeader("HX-Request").toBoolean(),
            isAdmin = isAdmin
        )
    }

    override fun fullPage(content: String, breadcrumbs: String, pagination: String, req: ServerRequest, isAdmin: Boolean): String {
        return viewRenderer.fullPage(
            content,
            breadcrumbs,
            pagination,
            !req.headers().firstHeader("HX-Request").toBoolean(),
            isAdmin
        )
    }

    override fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String {
        return viewRenderer.Pagination(currentPage, totalPages, baseUrl)
    }

    override fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String {
        return viewRenderer.IndeterminatePagination(currentPage, hasMoreItems, baseUrl)
    }

    override val contentType: MediaType = MediaType.TEXT_HTML

    override val baseUrl: String = "/api"
}
