package com.github.asm0dey.opdsko_spring.service

import com.github.asm0dey.opdsko.common.FormatConverter
import com.github.asm0dey.opdsko_spring.Book
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import java.io.File

/**
 * Service responsible for book format conversion operations.
 */
@Service
class BookConversionService(
    private val formatConverters: List<FormatConverter>,
    private val bookFileService: BookFileService,
    private val bookDataService: BookDataService
) {
    /**
     * Converts a book from one format to another.
     *
     * @param path The path to the book
     * @param targetFormat The target format
     * @return The converted book file, or null if conversion is not possible
     */
    fun convertBook(path: String, targetFormat: String): File? {
        val sourceFormat = path.substringAfterLast('.')
        val converter = formatConverters.firstOrNull {
            it.sourceFormat.equals(sourceFormat, ignoreCase = true) &&
                    it.targetFormat.equals(targetFormat, ignoreCase = true)
        } ?: return null
        if (!converter.canConvert(sourceFormat)) return null
        return bookFileService.getBookData(path).use { converter.convert(it) }
    }

    /**
     * Generates a file name for a book.
     *
     * @param book The book
     * @param extension The file extension
     * @return The generated file name
     */
    fun generateFileName(book: Book, extension: String): String {
        val baseFileName = buildString {
            append(book.name.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

            if (book.sequence != null) {
                append(" [")
                append(book.sequence.replace(Regex("[\\\\/:*?\"<>|]"), "_"))

                if (book.sequenceNumber != null) {
                    append(" #")
                    append(book.sequenceNumber)
                }

                append("]")
            }
        }

        return "$baseFileName.$extension"
    }

    /**
     * Gets the content type for a file extension.
     *
     * @param extension The file extension
     * @return The content type
     */
    fun getContentTypeForExtension(extension: String): String {
        return when (extension.lowercase()) {
            "epub" -> "application/epub+zip"
            "fb2" -> "application/x-fictionbook+xml"
            "pdf" -> "application/pdf"
            "mobi" -> "application/x-mobipocket-ebook"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    /**
     * Prepares a book for download, optionally converting it to a different format.
     *
     * @param bookId The ID of the book to download
     * @param targetFormat Optional format to convert the book to
     * @return OperationResultWithData containing the download data if successful
     */
    suspend fun downloadBook(bookId: String, targetFormat: String? = null): OperationResultWithData<BookDownloadData> {
        val book = bookDataService.getBookById(bookId) ?: return OperationResultWithData(OperationResult.NOT_FOUND)
        val bookPath = book.path
        val originalExtension = bookPath.substringAfterLast('.')

        if (targetFormat != null) {
            val convertedFile = convertBook(bookPath, targetFormat)
                ?: return OperationResultWithData(
                    OperationResult.BAD_REQUEST,
                    errorMessage = "Conversion to $targetFormat not supported"
                )

            val fileName = generateFileName(book, targetFormat)
            val contentType = getContentTypeForExtension(targetFormat)

            return OperationResultWithData(
                OperationResult.SUCCESS,
                BookDownloadData(
                    resource = FileSystemResource(convertedFile),
                    fileName = fileName,
                    contentType = contentType,
                    tempFile = convertedFile
                )
            )
        }

        val fileName = generateFileName(book, originalExtension)
        val contentType = getContentTypeForExtension(originalExtension)
        val bookData = InputStreamResource(bookFileService.getBookData(bookPath))

        return OperationResultWithData(
            OperationResult.SUCCESS,
            BookDownloadData(
                resource = bookData,
                fileName = fileName,
                contentType = contentType
            )
        )
    }
}
