package com.github.asm0dey.opdsko_spring

import com.mongodb.reactivestreams.client.MongoClient
import io.mongock.driver.mongodb.reactive.driver.MongoReactiveDriver
import io.mongock.runner.springboot.MongockSpringboot
import io.mongock.runner.springboot.base.MongockApplicationRunner
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
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
//@EnableMongock
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
class MongockConfig() {

    @Bean
    fun mongockApplicationRunner(
        springContext: ApplicationContext,
        mongoTemplate: MongoClient
    ): MongockApplicationRunner {
        val driver = MongoReactiveDriver.withDefaultLock(mongoTemplate, "mongock-lock")
        driver.enableTransaction()

        return MongockSpringboot.builder()
            .setDriver(driver)
            .addMigrationScanPackage("com.github.asm0dey.opdsko_spring.migrations")
            .setEventPublisher(springContext)
            .setSpringContext(springContext)
            .setTransactional(false)
            .setTrackIgnored(false)
            .buildApplicationRunner()
    }
}

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

            // Author navigation routes
            GET("/author", htmxHandler::authorFirstLevel)
            GET("/author/{prefix}", htmxHandler::authorPrefixLevel)
            GET("/author/view/{lastName}/{firstName}", htmxHandler::authorView)
            GET("/author/view/{lastName}/{firstName}/series", htmxHandler::authorSeries)
            GET("/author/view/{lastName}/{firstName}/series/{series}", htmxHandler::authorSeriesBooks)
            GET("/author/view/{lastName}/{firstName}/noseries", htmxHandler::authorNoSeriesBooks)
            GET("/author/view/{lastName}/{firstName}/all", htmxHandler::authorAllBooks)
        }
        GET("/opds/book/{id}/download", htmxHandler::downloadBook)
        GET("/opds/book/{id}/download/{format}", htmxHandler::downloadBook)
        GET("/opds/image/{id}", htmxHandler::getBookCover)
        GET("/opds/fullimage/{id}", htmxHandler::getFullBookCover)
        GET("/api/book/{id}/image", htmxHandler::getFullBookCover)
        GET("/api/book/{id}/info", htmxHandler::getBookInfo)
        POST("/scan", scanner::scan)
        POST("/cleanup", scanner::cleanup)
    }
}

@Component
class Scanner(
    val settings: ScannerSettings,
    private val bookRepo: BookRepo,
    private val bookService: BookService,
    private val meilisearch: Meilisearch,
    private val seaweedFSService: SeaweedFSService,
    private val bookMongoRepository: BookMongoRepository,
) {
    suspend fun scan(request: ServerRequest): ServerResponse {
        for (file in settings.sources) {
            val books = if (file.isDirectory) {
                file.walkTopDown()
                    .filter { it.isFile }
                    .flatMap { bookService.obtainBooks(it.absolutePath) }
            } else {
                // Process individual file
                bookService.obtainBooks(file.absolutePath)
            }

            val processedBooks = books
                .map { (commonBook, size) ->
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
                        size = size,
                        path = commonBook.path,
                        hasCover = commonBook.cover != null
                    )
                }
                .toList()

            if (processedBooks.isNotEmpty()) {
                // Save complete book information to MongoDB and get the saved books with MongoDB-generated IDs
                val savedBooksFlow = bookRepo.save(processedBooks)
                val savedBooks = savedBooksFlow.toList()

                // Save book names and IDs to Meilisearch using the MongoDB-generated IDs
                val bookIndexItems = savedBooks.map { BookIndexItem(it.path, it.name) }
                meilisearch.saveBooks(bookIndexItems)

                // Book covers will be retrieved and cached on demand, not during scan
            }
        }
        return ServerResponse.ok().buildAndAwait()
    }

    /**
     * Cleans up books that are no longer available in the sources.
     * For each source, scans it and removes books if they are not available anymore.
     * Uses book handlers and delegate book handlers to determine if books still exist.
     * 
     * @param request The server request
     * @return The server response
     */
    suspend fun cleanup(request: ServerRequest): ServerResponse {
        // Get all books from the database
        val allBooks = bookMongoRepository.findAll()

        val booksToRemove = allBooks
            .filter { !bookService.bookExists(it.path) }
            .toList()

        // Remove books that no longer exist
        if (booksToRemove.isNotEmpty()) {
            // Delete from MongoDB
            bookMongoRepository.deleteAll(booksToRemove)

            // Delete from Meilisearch
            meilisearch.deleteBooks(booksToRemove.map { it.id })

            // Delete book covers from SeaweedFS
            for (book in booksToRemove) {
                seaweedFSService.deleteBookCover(book.id)
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
    @Indexed(unique = true) val path: String,
    val hasCover: Boolean = true
)

data class Author(val lastName: String, val firstName: String)

