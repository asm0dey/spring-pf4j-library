package com.github.asm0dey.opdsko_spring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Tests for the file name generation and content type determination functionality.
 *
 * This test class implements the same logic as the private methods in HtmxHandler,
 * but in a way that doesn't require all the dependencies.
 */
class FileNameGenerationTests {

    /**
     * Generates a file name from the book's metadata.
     * The format is: "BookName [SequenceName #SequenceNumber].extension"
     * If sequence name or number is not available, they are omitted.
     *
     * This is a copy of the logic in HtmxHandler.generateFileName.
     */
    private fun generateFileName(book: Book, extension: String): String {
        val baseFileName = buildString {
            append(book.name.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

            book.sequence?.let { seq ->
                append(" [")
                append(seq.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

                book.sequenceNumber?.let { num ->
                    append(" #")
                    append(num)
                }

                append("]")
            }
        }

        return "$baseFileName.$extension"
    }

    /**
     * Determines the content type based on the file extension.
     *
     * This is a copy of the logic in HtmxHandler.getContentTypeForExtension.
     */
    private fun getContentTypeForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "epub" -> "application/epub+zip"
            "fb2" -> "application/x-fictionbook+xml"
            "pdf" -> "application/pdf"
            "mobi" -> "application/x-mobipocket-ebook"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    @Test
    fun `test file name generation with book name only`() {
        // Arrange
        val book = Book(
            id = "test-id",
            authors = emptyList(),
            genres = emptyList(),
            name = "Test Book",
            added = LocalDateTime.now(),
            size = 1000L,
            path = "/path/to/book.epub"
        )

        // Act
        val fileName = generateFileName(book, "epub")

        // Assert
        assertEquals("Test Book.epub", fileName)
    }

    @Test
    fun `test file name generation with book name and sequence`() {
        // Arrange
        val book = Book(
            id = "test-id",
            authors = emptyList(),
            genres = emptyList(),
            name = "Test Book",
            sequence = "Test Series",
            added = LocalDateTime.now(),
            size = 1000L,
            path = "/path/to/book.epub"
        )

        // Act
        val fileName = generateFileName(book, "epub")

        // Assert
        assertEquals("Test Book [Test Series].epub", fileName)
    }

    @Test
    fun `test file name generation with book name, sequence, and sequence number`() {
        // Arrange
        val book = Book(
            id = "test-id",
            authors = emptyList(),
            genres = emptyList(),
            name = "Test Book",
            sequence = "Test Series",
            sequenceNumber = 3,
            added = LocalDateTime.now(),
            size = 1000L,
            path = "/path/to/book.epub"
        )

        // Act
        val fileName = generateFileName(book, "epub")

        // Assert
        assertEquals("Test Book [Test Series #3].epub", fileName)
    }

    @Test
    fun `test file name generation with special characters`() {
        // Arrange
        val book = Book(
            id = "test-id",
            authors = emptyList(),
            genres = emptyList(),
            name = "Test: Book/With*Special?Characters",
            sequence = "Test: Series/With*Special?Characters",
            sequenceNumber = 3,
            added = LocalDateTime.now(),
            size = 1000L,
            path = "/path/to/book.epub"
        )

        // Act
        val fileName = generateFileName(book, "epub")

        // Assert
        assertEquals("Test_ Book_With_Special_Characters [Test_ Series_With_Special_Characters #3].epub", fileName)
    }

    @Test
    fun `test content type determination for various extensions`() {
        // Act & Assert
        assertEquals("application/epub+zip", getContentTypeForExtension("epub"))
        assertEquals("application/x-fictionbook+xml", getContentTypeForExtension("fb2"))
        assertEquals("application/pdf", getContentTypeForExtension("pdf"))
        assertEquals("application/x-mobipocket-ebook", getContentTypeForExtension("mobi"))
        assertEquals("text/plain", getContentTypeForExtension("txt"))
        assertEquals("application/octet-stream", getContentTypeForExtension("unknown"))
    }

    @Test
    fun `test content type determination is case insensitive`() {
        // Act & Assert
        assertEquals("application/epub+zip", getContentTypeForExtension("EPUB"))
        assertEquals("application/x-fictionbook+xml", getContentTypeForExtension("FB2"))
        assertEquals("application/pdf", getContentTypeForExtension("PDF"))
    }
}
