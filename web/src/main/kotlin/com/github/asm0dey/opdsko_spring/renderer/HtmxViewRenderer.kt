package com.github.asm0dey.opdsko_spring.renderer

import com.github.asm0dey.opdsko_spring.Book
import com.github.asm0dey.opdsko_spring.LibraryProperties
import kotlinx.html.*
import kotlinx.html.ButtonType.submit
import kotlinx.html.FormMethod.post
import kotlinx.html.stream.createHTML
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
class HtmxViewRenderer(private val libraryProperties: LibraryProperties) : ViewRenderer {
    override fun NavTile(title: String, subtitle: String, href: String): String {
        return createHTML(false).div("cell is-clickable") {
            layoutUpdateAttributes(href)
            article("box") {
                p("title") { +title }
                p("subtitle") { +subtitle }
            }
        }
    }

    override fun BookTile(
        book: Book,
        images: Map<String, String?>,
        descriptions: Map<String, String?>,
        additionalFormats: List<String>
    ): String {
        return createHTML(false).div("cell") {
            div("card") {
                div("card-header") {
                    p("card-header-title") {
                        +book.name
                    }
                }
                div("card-image") {
                    figure("image is-2by3") {
                        div {
                            attributes["hx-get"] = "/common/image/${book.id}"
                            attributes["hx-trigger"] = "load"
                            attributes["hx-swap"] = "outerHTML"
                            img(alt = "Book cover loading...", src = "/img/bars.svg") {
                                attributes["class"] = "htmx-indicator"
                                attributes["width"] = "30"
                            }
                        }
                    }
                }
                div("card-content") {
                    div {
                        attributes["hx-get"] = "/common/book/${book.id}/description"
                        attributes["hx-trigger"] = "load"
                        attributes["hx-swap"] = "outerHTML"
                        div("content htmx-indicator") {
                            +"Loading description..."
                        }
                    }
                }
                footer("card-footer mb-0 pb-0 is-align-items-self-end") {
                    a(classes = "card-footer-item") {
                        attributes["hx-get"] = "/common/book/${book.id}/info"
                        attributes["hx-target"] = "#modal-content"
                        attributes["hx-swap"] = "innerHTML"
                        attributes["_"] = "on htmx:afterOnLoad wait 10ms then add .is-active to #modal"
                        +"Info"
                    }

                    // Original format download
                    val extension = book.path.substringAfterLast('.')
                    a("/common/book/${book.id}/download", classes = "card-footer-item") {
                        +extension.uppercase()
                    }

                    additionalFormats.forEach {
                        a("/common/book/${book.id}/download/$it", classes = "card-footer-item") {
                            +it.uppercase()
                        }
                    }
                }
            }
        }
    }

    override fun Breadcrumbs(items: List<Pair<String, String>>): String {
        return createHTML(false).div {
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
    }

    val emptyNav = createHTML(false).div {
        attributes["hx-swap-oob"] = "innerHTML:.navv"
    }

    override fun fullPage(
        content: String,
        breadcrumbs: String,
        pagination: String,
        fullRender: Boolean,
        isAdmin: Boolean
    ) =
        if (!fullRender) content + breadcrumbs + (pagination.takeIf { it.isNotBlank() } ?: emptyNav)
        else fullPage(content, breadcrumbs, pagination, isAdmin)


    fun fullPage(content: String, breadcrumbs: String, pagination: String, isAdmin: Boolean = false): String {
        return createHTML(false).html {
            head {
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                title(libraryProperties.title)
                link(rel = "stylesheet", href = "/webjars/bulma/1.0.4/css/bulma.min.css")
                link(rel = "stylesheet", href = "/webjars/font-awesome/4.7.0/css/font-awesome.min.css")
            }
            body {
                nav(classes = "navbar") {
                    div("container") {
                        div("navbar-brand") {
                            a(classes = "navbar-item brand-text", href = "/api") {
                                attributes["hx-get"] = "/api"
                                attributes["hx-swap"] = "innerHTML show:.input:top"
                                attributes["hx-target"] = "#layout"
                                attributes["hx-indicator"] = "#loader"
                                img(alt = "Logo", src = "/logo.png")
                                +Entities.nbsp
                                +libraryProperties.title
                            }
                        }
                        div("navbar-end") {
                            div("navbar-item") {
                                div("buttons") {
                                    if (isAdmin) {
                                        form(action = "/scan", method = post, classes = "is-inline-block ml-2") {
                                            button(type = submit, classes = "button is-primary") { +"Scan" }
                                        }
                                        form(action = "/resync", method = post, classes = "is-inline-block ml-2") {
                                            button(type = submit, classes = "button is-info") { +"Resync" }
                                        }
                                        form(action = "/cleanup", method = post, classes = "is-inline-block ml-2") {
                                            button(type = submit, classes = "button is-warning") { +"Cleanup" }
                                        }
                                    }
                                    form(action = "/logout", method = post) {
                                        button(type = submit, classes = "button is-danger") {
                                            +"Logout"
                                        }
                                    }
                                }
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
                                    attributes["hx-indicator"] = "#loader"
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
                                    attributes["hx-indicator"] = "#loader"
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
                    div("fixed-grid has-3-cols has-1-cols-mobile") {
                        div("grid") {
                            id = "layout"
                            unsafe {
                                +content
                            }
                        }
                    }
                    div(classes = "navv") {
                        unsafe {
                            +pagination
                        }
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
                }
                script(src = "/webjars/htmx.org/2.0.4/dist/htmx.min.js") {}
                script(src = "/webjars/htmx-ext-sse/2.2.3/dist/sse.min.js") {}
                script(src = "/webjars/hyperscript.org/0.9.13/dist/_hyperscript.min.js") {}
            }
        }
    }

    override fun Pagination(currentPage: Int, totalPages: Int, baseUrl: String): String {
        return createHTML(false).div {
            attributes["hx-swap-oob"] = "innerHTML:.navv"
            nav {
                classes = setOf("pagination", "is-centered")
                role = "navigation"
                a {
                    classes = setOfNotNull("pagination-previous", if (currentPage == 1) "is-disabled" else null)
                    if (currentPage != 1) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = "$baseUrl?page=${currentPage - 1}"
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                        attributes["hx-indicator"] = "#loader"
                    }
                    +"Previous page"
                }
                a {
                    val last = totalPages / 15 + 1
                    classes = setOfNotNull("pagination-next", if (currentPage == last) "is-disabled" else null)
                    if (currentPage != last) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = "$baseUrl?page=${currentPage + 1}"
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                        attributes["hx-indicator"] = "#loader"
                    }
                    +"Next page"
                }
            }
        }
    }

    override fun IndeterminatePagination(currentPage: Int, hasMoreItems: Boolean, baseUrl: String): String {
        return createHTML(false).div {
            attributes["hx-swap-oob"] = "innerHTML:.navv"
            nav {
                classes = setOf("pagination", "is-centered")
                role = "navigation"
                a {
                    classes = setOfNotNull("pagination-previous", if (currentPage == 1) "is-disabled" else null)
                    if (currentPage != 1) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = "$baseUrl?page=${currentPage - 1}"
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                        attributes["hx-indicator"] = "#loader"
                    }
                    +"Previous page"
                }
                a {
                    classes = setOfNotNull("pagination-next", if (!hasMoreItems) "is-disabled" else null)
                    if (hasMoreItems) {
                        attributes["hx-trigger"] = "click"
                        attributes["hx-get"] = "$baseUrl?page=${currentPage + 1}"
                        attributes["hx-swap"] = "innerHTML show:.input:top"
                        attributes["hx-target"] = "#layout"
                        attributes["hx-push-url"] = "true"
                        attributes["hx-indicator"] = "#loader"
                    }
                    +"Next page"
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
        attributes["hx-indicator"] = "#loader"
    }
}
