package com.github.asm0dey.opdsko_spring.model

import com.github.asm0dey.opdsko_spring.Book

data class NavTileViewModel(val title: String, val subtitle: String, val href: String)
data class BookTileViewModel(val book: Book, val coverUrl: String?, val description: String?)
data class BreadcrumbsViewModel(val items: List<Pair<String, String>>)
