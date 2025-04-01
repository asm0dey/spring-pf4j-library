package com.github.asm0dey.opdsko_spring.renderer

import com.github.asm0dey.opdsko_spring.Book

@Suppress("FunctionName")
interface ViewRenderer {
    fun NavTile(title: String, subtitle: String, href: String): String
    fun BookTile(
        book: Book,
        images: Map<String, String?>,
        descriptions: Map<String, String?>,
        additionalFormats: List<String>
    ): String
    fun Breadcrumbs(items: List<Pair<String, String>>): String
    fun fullPage(content: String, breadcrumbs: String, pagination: String = "", fullRender: Boolean = true, isAdmin: Boolean = false): String
    fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String
    fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String
}
