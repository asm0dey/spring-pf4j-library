package com.github.asm0dey.opdsko_spring

import generated.jooq.tables.interfaces.IAuthor
import generated.jooq.tables.interfaces.IBook
import generated.jooq.tables.pojos.Author
import generated.jooq.tables.pojos.Book
import generated.jooq.tables.references.BOOK
import org.jooq.Record2
import org.jooq.Record5
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.BindingReflectionHintsRegistrar
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.io.File
import java.io.Serializable
import java.net.URI
import java.util.function.Consumer
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement
import kotlin.LazyThreadSafetyMode.PUBLICATION


@SpringBootApplication(scanBasePackages = ["com.github.asm0dey.opdsko_spring", "generated.jooq.tables.daos"])
@ImportRuntimeHints(MyRuntimeHintsRegistrar::class)
class OpdskoSpringApplication

fun main(args: Array<String>) {
    runApplication<OpdskoSpringApplication>(*args)
}

class MyRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    private val log = LoggerFactory.getLogger(this::class.java)
    private var packagesToScan = listOf(
        "generated.jooq",
        "org.flywaydb.core.internal.configuration.extensions",
        "org.flywaydb.core.internal.publishing"
    )

    private val bindingReflectionHintsRegistrar = BindingReflectionHintsRegistrar()


    override fun registerHints(hint: RuntimeHints, classLoader: ClassLoader?) {
        for (packageName in packagesToScan) {
            registerPackage(hint, packageName)
        }
    }

    private fun registerPackage(hint: RuntimeHints, packageName: String) {
        val reflections = Reflections(packageName, Scanners.SubTypes.filterResultsBy { true })
        val allTypes = reflections.getSubTypesOf(Any::class.java)
        log.info("Found " + allTypes.size + " classes for package " + packageName)
        allTypes.stream().map { it: Class<*> -> it.name }.sorted().forEach { it: String ->
            log.info("Registering $it")
        }
        allTypes.forEach(Consumer { type: Class<*>? ->
            // Reusing behavior of @RRegisterReflectionForBinding annotation
            bindingReflectionHintsRegistrar.registerReflectionHints(hint.reflection(), type)
        })
        reflections.getSubTypesOf(Serializable::class.java).forEach(Consumer { type: Class<out Serializable?>? ->
            hint.serialization().registerType(type!!)
        })
    }
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
        POST("/scan", scanner::scan)
    }
}

@Component
class Scanner(
    val settings: ScannerSettings,
    private val bookRepo: BookRepo,
    private val bookService: BookService,
) {
    suspend fun scan(request: ServerRequest): ServerResponse {
        for (file in settings.sources) {
            if (file.isDirectory) {
                val books = file.walkTopDown().filter { it.isFile }
                    .flatMap { bookService.obtainBooks(it.absolutePath) }
                    .filterNotNull()
                    .toList()
//                bookRepo.save(toList)
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

@JvmInline
@Suppress("unused")
value class BookWithInfo(private val record: Record5<Long?, MutableList<Book>, MutableList<Author>, List<Record2<String?, Long?>>, String?>) {
    val book: IBook
        get() = record.component2()[0]
    val authors: List<IAuthor>
        get() = record.component3()!!
    val genres: List<Pair<String, Long>>
        get() = record.component4().map {
            val id = it.component2()!!
            val name = genreNames[it.component1()] ?: it.component1()!!
            name to id
        }
    val id
        get() = record.get(BOOK.ID)!!
    val sequence
        get() = record.get(BOOK.SEQUENCE)

    operator fun component1(): IBook = book
    operator fun component2(): List<IAuthor> = authors
    operator fun component3(): List<Pair<String, Long>> = genres
    operator fun component4(): Long = id
    operator fun component5(): String? = sequence
}

