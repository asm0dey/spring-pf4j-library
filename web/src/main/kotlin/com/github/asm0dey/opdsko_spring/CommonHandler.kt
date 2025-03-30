package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko_spring.OperationResult.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.net.URLEncoder
import java.nio.charset.Charset

@Component
class CommonHandler(private val bookService: BookService) {

    suspend fun downloadBook(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val targetFormat = req.pathVariableOrNull("format")

        val result = bookService.downloadBook(bookId, targetFormat)

        return when (result.result) {
            NOT_FOUND -> ServerResponse.notFound().buildAndAwait()
            BAD_REQUEST -> ServerResponse.badRequest().bodyValueAndAwait(result.errorMessage ?: "Bad request")
            SUCCESS -> {
                val downloadData = result.data!!
                val headers = HttpHeaders().apply {
                    contentDisposition = ContentDisposition.attachment()
                        .filename(downloadData.fileName, Charset.forName("UTF-8"))
                        .build()
                }

                ok()
                    .headers { it.addAll(headers) }
                    .contentType(MediaType.parseMediaType(downloadData.contentType))
                    .bodyValueAndAwait(downloadData.resource)
                    .also { downloadData.tempFile?.parentFile?.deleteRecursively() }
            }
        }
    }

    suspend fun getBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val result = bookService.getBookCover(bookId)

        return handleBookCoverRequest(result)
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
                        img(src = "/common/fullimage/${book.id}") {
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
                            a(
                                href = "/api/author/view/${
                                    URLEncoder.encode(
                                        author.fullName,
                                        Charset.defaultCharset()
                                    )
                                }",
                                classes = "tag is-info"
                            ) {
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

    suspend fun getFullBookCover(req: ServerRequest): ServerResponse {
        val bookId = req.pathVariable("id")
        val isHtmxRequest = req.headers().firstHeader("HX-Request") == "true"

        if (isHtmxRequest) {
            val html = createHTML().div("box") {
                figure("image") {
                    img(src = "/common/fullimage/${bookId}") {
                        attributes["loading"] = "lazy"
                        attributes["alt"] = "Full-size book cover"
                    }
                }
            }

            return ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValueAndAwait(html)
        } else {
            val result = bookService.getFullBookCover(bookId)

            return handleBookCoverRequest(result)
        }
    }

    private suspend fun handleBookCoverRequest(result: OperationResultWithData<BookCoverData>): ServerResponse {
        return when (result.result) {
            NOT_FOUND -> ServerResponse.notFound().buildAndAwait()
            SUCCESS -> {
                val bookCoverData = result.data!!
                val inputStreamResource = InputStreamResource(bookCoverData.inputStream)
                ok()
                    .contentType(MediaType.parseMediaType(bookCoverData.contentType))
                    .bodyValueAndAwait(inputStreamResource)
            }

            else -> ServerResponse.badRequest().buildAndAwait()
        }
    }
}