package com.github.asm0dey.opdsko_spring

import com.mongodb.reactivestreams.client.MongoClient
import io.mongock.driver.mongodb.reactive.driver.MongoReactiveDriver
import io.mongock.runner.springboot.MongockSpringboot
import io.mongock.runner.springboot.base.MongockApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.*
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement
import kotlin.LazyThreadSafetyMode.PUBLICATION


@SpringBootApplication
class OpdskoSpringApplication

fun main(args: Array<String>) {
    runApplication<OpdskoSpringApplication>(*args)
}

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

@Document
data class Book(
    val authors: List<Author>,
    var genres: List<String>,
    val sequence: String? = null,
    var sequenceNumber: Int? = null,
    val name: String,
    val added: LocalDateTime = LocalDateTime.now(),
    val size: Long,
    @Indexed(unique = true) val path: String,
    @Id val id: String = UUID.nameUUIDFromBytes(path.toByteArray()).toString(),
    val hasCover: Boolean = true
)

data class Author(
    override val lastName: String,
    override val firstName: String,
    override val middleName: String? = null,
    override val nickname: String? = null,
    @Indexed val fullName: String = "$lastName, $firstName"
) : com.github.asm0dey.opdsko.common.Author {
}
