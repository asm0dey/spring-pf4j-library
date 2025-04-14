package com.github.asm0dey.opdsko_spring

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import javax.xml.namespace.QName
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement
import kotlin.LazyThreadSafetyMode.PUBLICATION


@SpringBootApplication
class OpdskoSpringApplication

fun main(args: Array<String>) {
    val cliCommands = listOf("--add-user", "--update-password", "--set-role")
    val isCliCommand = args.any { arg -> cliCommands.any { cmd -> arg.startsWith(cmd) } }

    if (isCliCommand) {
        // Run the application without starting the web server
        SpringApplicationBuilder(OpdskoSpringApplication::class.java)
            .web(WebApplicationType.NONE)
            .run(*args)
    } else {
        runApplication<OpdskoSpringApplication>(*args)
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
    @Indexed val added: LocalDateTime = LocalDateTime.now(),
    val size: Long,
    @Indexed(unique = true) val path: String,
    @Id val id: String? = null,
    val hasCover: Boolean = true
)

data class Author(
    override val lastName: String,
    override val firstName: String,
    override val middleName: String? = null,
    override val nickname: String? = null,
    @Indexed val fullName: String = "$lastName, $firstName"
) : com.github.asm0dey.opdsko.common.Author
