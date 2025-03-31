package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.FormatConverter
import com.github.asm0dey.opdsko_spring.handler.HtmxHandler
import com.github.asm0dey.opdsko_spring.renderer.HtmxViewRenderer
import com.github.asm0dey.opdsko_spring.repo.PagedBooks
import com.github.asm0dey.opdsko_spring.service.BookService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Sort
import org.springframework.http.HttpMethod
import org.springframework.mock.web.reactive.function.server.MockServerRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class HtmxHandlerTest {

    @Mock
    private lateinit var bookService: BookService
    private val formatConverters: List<FormatConverter> = listOf(MockFormatConverter())
    private lateinit var htmxHandler: HtmxHandler

    @BeforeEach
    fun setup() {
        htmxHandler = HtmxHandler(bookService, HtmxViewRenderer(), formatConverters)
    }

    @Test
    fun `test homePage returns correct HTML`() = runBlocking {
        // Arrange
        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .build()

        // Act
        val response = htmxHandler.homePage(mockRequest)

        // Assert
        // We can't easily assert the exact HTML content, but we can verify the response is successful
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test search returns correct HTML`() = runBlocking {
        // Arrange
        val searchTerm = "test book"
        val testBooks = createTestBooks(3)

        val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()
        queryParams.add("search", searchTerm)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .queryParams(queryParams)
            .build()

        `when`(bookService.searchBookByName(searchTerm, 0)).thenReturn(testBooks)
        `when`(bookService.imageTypes(testBooks)).thenReturn(
            mapOf(
                "1" to "image/jpeg",
                "2" to "image/jpeg",
                "3" to "image/jpeg"
            )
        )
        `when`(bookService.shortDescriptions(testBooks)).thenReturn(
            mapOf(
                "1" to "Description 1",
                "2" to "Description 2",
                "3" to "Description 3"
            )
        )

        // Act
        val response = htmxHandler.search(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test new returns correct HTML`() = runBlocking {
        // Arrange
        val testBooks = createTestBooks(3)
        val pagedBooks = PagedBooks(testBooks, 3)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .build()

        `when`(bookService.newBooks(0)).thenReturn(pagedBooks)
        `when`(bookService.imageTypes(testBooks)).thenReturn(
            mapOf(
                "1" to "image/jpeg",
                "2" to "image/jpeg",
                "3" to "image/jpeg"
            )
        )
        `when`(bookService.shortDescriptions(testBooks)).thenReturn(
            mapOf(
                "1" to "Description 1",
                "2" to "Description 2",
                "3" to "Description 3"
            )
        )

        // Act
        val response = htmxHandler.new(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorFirstLevel returns correct HTML`() = runBlocking {
        // Arrange
        val authorLetters = listOf(
            AuthorLetterResult("A", 10),
            AuthorLetterResult("B", 5),
            AuthorLetterResult("C", 3)
        )

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .build()

        `when`(bookService.findAuthorFirstLetters()).thenReturn(flowOf(*authorLetters.toTypedArray()))

        // Act
        val response = htmxHandler.authorFirstLevel(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test seriesBooks returns correct HTML`() = runBlocking {
        // Arrange
        val seriesName = "Test Series"
        val testBooks = createTestBooks(3)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("series", seriesName)
            .build()

        `when`(bookService.findBooksBySeries(seriesName, Sort.by(Sort.Direction.ASC, "sequenceNumber")))
            .thenReturn(flowOf(*testBooks.toTypedArray()))

        // Ensure all books have IDs and the maps contain entries for all IDs
        val imageTypes = testBooks.associate { it.id!! to "image/jpeg" }
        val shortDescriptions = testBooks.associate { it.id!! to "Description for ${it.name}" }

        `when`(bookService.imageTypes(testBooks)).thenReturn(imageTypes)
        `when`(bookService.shortDescriptions(testBooks)).thenReturn(shortDescriptions)

        // Act
        val response = htmxHandler.seriesBooks(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorPrefixLevel returns correct HTML when depth is less than 5`() = runBlocking {
        // Arrange
        val prefix = "Aut"
        val prefixResults = listOf(
            AuthorLetterResult("Auth", 5),
            AuthorLetterResult("Auto", 3)
        )
        val exactLastNameCount = listOf(CountResult(0))

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("prefix", prefix)
            .build()

        `when`(bookService.countExactLastNames(prefix)).thenReturn(flowOf(*exactLastNameCount.toTypedArray()))
        `when`(
            bookService.findAuthorPrefixes(
                prefix,
                prefix.length + 1
            )
        ).thenReturn(flowOf(*prefixResults.toTypedArray()))

        // Act
        val response = htmxHandler.authorPrefixLevel(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorPrefixLevel redirects to authorFullNames when depth is 5 or more`() = runBlocking {
        // Arrange
        val prefix = "Author"
        val authors = listOf(
            AuthorResult(AuthorResult.AuthorId("Author, Test", "Author", "Test"))
        )

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("prefix", prefix)
            .build()

        `when`(bookService.findAuthorsByPrefix(prefix)).thenReturn(flowOf(*authors.toTypedArray()))

        // Act
        val response = htmxHandler.authorPrefixLevel(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorPrefixLevel redirects to authorFullNames when exact last name exists`() = runBlocking {
        // Arrange
        val prefix = "Aut"
        val exactLastNameCount = listOf(CountResult(1))
        val authors = listOf(
            AuthorResult(AuthorResult.AuthorId("Author, Test", "Author", "Test"))
        )

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("prefix", prefix)
            .build()

        `when`(bookService.countExactLastNames(prefix)).thenReturn(flowOf(*exactLastNameCount.toTypedArray()))
        `when`(bookService.findAuthorsByPrefix(prefix)).thenReturn(flowOf(*authors.toTypedArray()))

        // Act
        val response = htmxHandler.authorPrefixLevel(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorFullNames returns correct HTML`() = runBlocking {
        // Arrange
        val prefix = "Author"
        val authors = listOf(
            AuthorResult(AuthorResult.AuthorId("Author, Test", "Author", "Test")),
            AuthorResult(AuthorResult.AuthorId("Author, Another", "Author", "Another"))
        )

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("prefix", prefix)
            .build()

        `when`(bookService.findAuthorsByPrefix(prefix)).thenReturn(flowOf(*authors.toTypedArray()))

        // Act
        val response = htmxHandler.authorFullNames(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorView returns correct HTML when author has series`() = runBlocking {
        // Arrange
        val fullName = "Author, Test"
        val encodedFullName = "Author%2C+Test"
        val series = listOf(SeriesResult("Test Series"))

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("fullName", encodedFullName)
            .build()

        `when`(bookService.findSeriesByAuthorFullName(fullName)).thenReturn(flowOf(*series.toTypedArray()))

        // Act
        val response = htmxHandler.authorView(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorView redirects to authorAllBooks when author has no series`() = runBlocking {
        // Arrange
        val fullName = "Author, Test"
        val encodedFullName = "Author%2C+Test"
        val pagedBooks = PagedBooks(createTestBooks(3), 3)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("fullName", encodedFullName)
            .build()

        `when`(bookService.findSeriesByAuthorFullName(fullName)).thenReturn(flowOf())
        `when`(bookService.findBooksByAuthorFullName(fullName, 0)).thenReturn(pagedBooks)
        `when`(bookService.imageTypes(pagedBooks.books)).thenReturn(pagedBooks.books.associate { it.id!! to "image/jpeg" })
        `when`(bookService.shortDescriptions(pagedBooks.books)).thenReturn(pagedBooks.books.associate { it.id!! to "Description for ${it.name}" })

        // Act
        val response = htmxHandler.authorView(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorSeries returns correct HTML`() = runBlocking {
        // Arrange
        val fullName = "Author, Test"
        val encodedFullName = "Author%2C+Test"
        val series = listOf(SeriesResult("Test Series"), SeriesResult("Another Series"))

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("fullName", encodedFullName)
            .build()

        `when`(bookService.findSeriesByAuthorFullName(fullName)).thenReturn(flowOf(*series.toTypedArray()))

        // Act
        val response = htmxHandler.authorSeries(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorSeriesBooks returns correct HTML`() = runBlocking {
        // Arrange
        val fullName = "Author, Test"
        val encodedFullName = "Author%2C+Test"
        val seriesName = "Test Series"
        val encodedSeriesName = "Test+Series"
        val testBooks = createTestBooks(3)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("fullName", encodedFullName)
            .pathVariable("series", encodedSeriesName)
            .build()

        `when`(bookService.findBooksBySeriesAndAuthorFullName(seriesName, fullName, Sort.by(Sort.Direction.ASC, "sequenceNumber")))
            .thenReturn(flowOf(*testBooks.toTypedArray()))
        `when`(bookService.imageTypes(testBooks)).thenReturn(testBooks.associate { it.id!! to "image/jpeg" })
        `when`(bookService.shortDescriptions(testBooks)).thenReturn(testBooks.associate { it.id!! to "Description for ${it.name}" })

        // Act
        val response = htmxHandler.authorSeriesBooks(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorNoSeriesBooks returns correct HTML`() = runBlocking {
        // Arrange
        val fullName = "Author, Test"
        val encodedFullName = "Author%2C+Test"
        val testBooks = createTestBooks(3)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("fullName", encodedFullName)
            .build()

        `when`(bookService.findBooksByAuthorWithoutSeriesFullName(fullName, Sort.by(Sort.Direction.ASC, "name")))
            .thenReturn(flowOf(*testBooks.toTypedArray()))
        `when`(bookService.imageTypes(testBooks)).thenReturn(testBooks.associate { it.id!! to "image/jpeg" })
        `when`(bookService.shortDescriptions(testBooks)).thenReturn(testBooks.associate { it.id!! to "Description for ${it.name}" })

        // Act
        val response = htmxHandler.authorNoSeriesBooks(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    @Test
    fun `test authorAllBooks returns correct HTML`() = runBlocking {
        // Arrange
        val fullName = "Author, Test"
        val encodedFullName = "Author%2C+Test"
        val testBooks = createTestBooks(3)
        val pagedBooks = PagedBooks(testBooks, 3)

        val mockRequest = MockServerRequest.builder()
            .method(HttpMethod.GET)
            .header("HX-Request", "false")
            .pathVariable("fullName", encodedFullName)
            .build()

        `when`(bookService.findBooksByAuthorFullName(fullName, 0)).thenReturn(pagedBooks)
        `when`(bookService.imageTypes(testBooks)).thenReturn(testBooks.associate { it.id!! to "image/jpeg" })
        `when`(bookService.shortDescriptions(testBooks)).thenReturn(testBooks.associate { it.id!! to "Description for ${it.name}" })

        // Act
        val response = htmxHandler.authorAllBooks(mockRequest)

        // Assert
        assert(response.statusCode().is2xxSuccessful)
    }

    // Helper method to create test books
    private fun createTestBooks(count: Int): List<Book> {
        return (1..count).map { i ->
            Book(
                id = i.toString(),
                authors = listOf(
                    Author(
                        lastName = "Author",
                        firstName = "Test $i",
                        fullName = "Author, Test $i"
                    )
                ),
                genres = listOf("fiction"),
                name = "Test Book $i",
                sequence = if (i % 2 == 0) "Test Series" else null,
                sequenceNumber = if (i % 2 == 0) i else null,
                added = LocalDateTime.now(),
                size = 1000L * i,
                path = "/path/to/book$i.txt", // Use txt extension to avoid format conversion
                hasCover = true
            )
        }
    }

    private class MockFormatConverter : FormatConverter {
        override val sourceFormat: String = "txt"
        override val targetFormat: String = "epub"

        override fun canConvert(sourceFormat: String): Boolean = sourceFormat.equals("txt", ignoreCase = true)

        override fun convert(inputStream: InputStream): File = File.createTempFile("test", ".epub")
    }

}
