package com.github.asm0dey.opdsko_spring

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.io.File
import java.net.URI
import java.time.LocalDateTime
import java.util.*
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement
import kotlin.LazyThreadSafetyMode.PUBLICATION


@SpringBootApplication
//@ImportRuntimeHints(MyRuntimeHintsRegistrar::class)
class OpdskoSpringApplication

fun main(args: Array<String>) {
    runApplication<OpdskoSpringApplication>(*args)
}


@Suppress("ArrayInDataClass")
@Configuration
@ConfigurationProperties(prefix = "scanner")
data class ScannerSettings(var sources: Array<File> = arrayOf())

@Configuration
class RoutingConfig {

    @Bean
    fun router(htmxHandler: HtmxHandler, scanner: Scanner) = coRouter {
        GET("/") { ServerResponse.permanentRedirect(URI("/api")).buildAndAwait() }
        resources("/**", ClassPathResource("static/"))
        GET("/api").nest {
            GET("", htmxHandler::homePage)
            GET("/search", htmxHandler::search)
            GET("/new/{page}", htmxHandler::new)
        }
        GET("/opds/book/{id}/download", htmxHandler::downloadBook)
        GET("/opds/book/{id}/download/{format}", htmxHandler::downloadBook)
        POST("/scan", scanner::scan)
    }
}

@Component
class Scanner(
    val settings: ScannerSettings,
    private val bookRepo: BookRepo,
    private val bookService: BookService,
    private val meilisearch: Meilisearch,
) {
    suspend fun scan(request: ServerRequest): ServerResponse {
        for (file in settings.sources) {
            if (file.isDirectory) {
                val books = file.walkTopDown()
                    .filter { it.isFile }
                    .flatMap { bookService.obtainBooks(it.absolutePath) }
                    .filterNotNull()
                    .map { commonBook ->
                        Book(
                            authors = commonBook.authors.map {
                                Author(
                                    lastName = it.lastName ?: "",
                                    firstName = it.firstName ?: ""
                                )
                            },
                            genres = commonBook.genres,
                            sequence = commonBook.sequenceName,
                            sequenceNumber = commonBook.sequenceNumber,
                            name = commonBook.title,
                            size = File(commonBook.path).length(),
                            path = commonBook.path
                        )
                    }
                    .toList()

                // Save complete book information to MongoDB and get the saved books with MongoDB-generated IDs
                val savedBooks = bookRepo.save(books)

                // Save book names and IDs to Meilisearch using the MongoDB-generated IDs
                val bookIndexItems = savedBooks.map { BookIndexItem(it.path, it.name) }.toList()
                meilisearch.saveBooks(bookIndexItems)
            }
        }
        return ServerResponse.ok().buildAndAwait()
    }
}

val genreNames by lazy(PUBLICATION) {
    fun StartElement.getAttributeValue(attribute: String) =
        getAttributeByName(QName.valueOf(attribute)).value

    OpdskoSpringApplication::class.java.classLoader.getResourceAsStream("genres.xml")?.buffered().use {
        val reader = XMLInputFactory.newDefaultFactory().createXMLEventReader(it)
        var currentGenre = ""
        val data = hashMapOf<String, String>()
        while (reader.hasNext()) {
            val nextEvent = reader.nextEvent()
            when {
                nextEvent.isStartElement -> {
                    val startElement = nextEvent.asStartElement()
                    when (startElement.name.localPart) {
                        "genre" -> currentGenre = startElement.getAttributeValue("value")
                        "root-descr" ->
                            if (startElement.getAttributeValue("lang") == "en")
                                data[currentGenre] =
                                    startElement.getAttributeValue("genre-title")

                        "subgenre" -> currentGenre = startElement.getAttributeValue("value")
                        "genre-descr" ->
                            if (startElement.getAttributeValue("lang") == "en")
                                data[currentGenre] = startElement.getAttributeValue("title")

                        "genre-alt" ->
                            data[startElement.getAttributeValue("value")] = data[currentGenre]!!
                    }
                }

                nextEvent.isEndElement ->
                    if (nextEvent.asEndElement().name.localPart == "genre") currentGenre = ""
            }
        }
        data
    }

}

typealias BookWithInfo = Book

@Document
data class Book(
    @Id val id: String = UUID.randomUUID().toString(),
    val authors: List<Author>,
    var genres: List<String>,
    val sequence: String? = null,
    var sequenceNumber: Int? = null,
    val name: String,
    val added: LocalDateTime = LocalDateTime.now(),
    val size: Long,
    @Indexed(unique = true) val path: String
)

data class Author(val lastName: String, val firstName: String)
