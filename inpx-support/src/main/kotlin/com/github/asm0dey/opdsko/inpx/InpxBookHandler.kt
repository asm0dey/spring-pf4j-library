package com.github.asm0dey.opdsko.inpx

import com.github.asm0dey.opdsko.common.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import net.lingala.zip4j.ZipFile
import org.pf4j.Extension
import java.io.File
import java.io.InputStream

const val INPX_PREFIX = "inpx::"
const val INPX_DELIMITER = ".inpx#"
const val INPX_EXTENSION = ".inpx"
const val INP_DELIMITER = "*"

/**
 * Data class to hold the components of an INPX path
 */
data class InpxPathComponents(
    val inpxPath: String,
    val inpFileName: String,
    val bookPath: String
)

/**
 * Creates a path string from the given components
 */
fun createInpxPath(inpxPath: String, inpFileName: String, bookPath: String, format: String): String {
    return "$INPX_PREFIX$inpxPath$INPX_DELIMITER$inpFileName$INP_DELIMITER$bookPath.$format"
}

/**
 * Parses a path string into its components
 */
fun parseInpxPath(path: String): InpxPathComponents {
    val inpxPath = path.substringAfter(INPX_PREFIX).substringBefore(INPX_DELIMITER)
    val afterDelimiter = path.substringAfter(INPX_DELIMITER)
    val inpFileName = afterDelimiter.substringBefore(INP_DELIMITER)
    val bookPath = afterDelimiter.substringAfter(INP_DELIMITER)
    return InpxPathComponents(inpxPath, inpFileName, bookPath)
}

@Extension(points = [DelegatingBookHandler::class])
class InpxBookHandler : DelegatingBookHandler {

    override fun supportFile(file: File) = file.name.endsWith(INPX_EXTENSION)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun obtainBooks(file: File, handlers: Collection<BookHandler>): Flow<Pair<Book, Long>> {
        return ZipFile(file).use { zip ->
            zip
                .fileHeaders
                .filter {
                    it.fileName.endsWith(".inp")
                }
                .asFlow()
                .flatMapConcat {
                    parseInpFile(zip.getInputStream(it), file.absolutePath, it.fileName)
                }
        }
    }

    fun parseInpFile(
        inputStream: InputStream,
        inpxPath: String,
        inpFileName: String
    ): Flow<Pair<Book, Long>> = flow {
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    val parts = line.split("\u0004")
                    if (parts.size >= 14) {
                        // INPX format with these fields:
                        // 0: AUTHOR
                        // 1: GENRE
                        // 2: TITLE
                        // 3: SERIES
                        // 4: SERNO (Series Number)
                        // 5: FILE (File name)
                        // 6: SIZE
                        // 7: LIBID
                        // 8: DEL
                        // 9: EXT (File format)
                        // 10: DATE
                        // 11: LANG
                        // 12: LIBRATE
                        // 13: KEYWORDS

                        val authors = parseAuthors(parts.getOrNull(0) ?: "")
                        val genres = parseGenres(parts.getOrNull(1) ?: "")
                        val title = parts.getOrNull(2) ?: ""
                        val series = parts.getOrNull(3)
                        val seriesNumber = parts.getOrNull(4)?.toIntOrNull()
                        val fileName = parts.getOrNull(5) ?: ""
                        val del = parts.getOrNull(8) ?: ""
                        val format = parts.getOrNull(9) ?: ""

                        if (title.isNotBlank() && fileName.isNotBlank() && del != "1") {
                            val book = InpxBook(
                                title = title,
                                authors = authors,
                                genres = genres,
                                sequenceName = if (series.isNullOrBlank()) null else series,
                                sequenceNumber = seriesNumber,
                                path = createInpxPath(inpxPath, inpFileName, fileName, format),
                                cover = byteArrayOf(), // avoid parsing fb2 to extract cover, will do on demand
                            )
                            val resolve =
                                File(inpxPath).parentFile.resolve(File(inpFileName).nameWithoutExtension + ".zip")
                            if (resolve.exists()) {
                                // Return 0 for size instead of reading it immediately
                                emit(book to 0L)
                            }
                        }
                    }
                }
            }
        }
    }

    fun parseAuthors(authorsString: String): List<Author> {
        // If the string is empty or blank, return an empty list
        if (authorsString.isBlank()) return emptyList()

        // Remove trailing colon if present
        val cleanedString = if (authorsString.endsWith(":")) authorsString.dropLast(1) else authorsString

        // Split by colon to get author groups
        return cleanedString.split(":").mapNotNull { authorGroup ->
            val trimmedName = authorGroup.trim()
            if (trimmedName.isBlank()) return@mapNotNull null

            // Split by comma to get name parts
            val parts = trimmedName.split(",").map { it.trim() }

            when (parts.size) {
                1 -> AuthorAdapter(lastName = parts[0])
                2 -> AuthorAdapter(lastName = parts[0], firstName = parts[1])
                3 -> AuthorAdapter(lastName = parts[0], firstName = parts[1], middleName = parts[2])
                else -> AuthorAdapter(
                    lastName = parts[0],
                    firstName = parts[1],
                    middleName = parts.subList(2, parts.size).joinToString(", ")
                )
            }
        }
    }

    fun parseGenres(genresString: String): List<String> {
        return genresString.split(":").map { it.trim() }.filter { it.isNotBlank() }
    }

    override fun supportsPath(path: String) =
        path.startsWith(INPX_PREFIX) && path.contains(INPX_DELIMITER) && path.substringAfter(INPX_DELIMITER)
            .contains(INP_DELIMITER)

    override fun obtainBook(path: String, handlers: Collection<BookHandler>): Book {
        val components = parseInpxPath(path)
        val inpxPath = components.inpxPath + INPX_EXTENSION
        val inpFileName = components.inpFileName
        val bookPath = components.bookPath

        // Find the ZIP file next to the INPX file with the same name but .zip extension
        val inpxFile = File(inpxPath)
        val zipFileName = File(inpFileName).nameWithoutExtension
        val zipFile = File(inpxFile.parent, "$zipFileName.zip")

        if (!zipFile.exists()) {
            // If the ZIP file doesn't exist, throw an error
            error("ZIP file not found for INPX: $inpxPath")
        }

        // Extract the file with the name specified in the bookPath from the ZIP file
        return ZipFile(zipFile).use { zip ->
            val fileHeader = zip.fileHeaders.find { it.fileName == bookPath }
            if (fileHeader == null) {
                // If the file doesn't exist in the ZIP, throw an error
                error("File not found in ZIP: $bookPath")
            } else {
                // Find a handler that supports this file
                val handler = handlers
                    .firstOrNull { it.supportsFile(bookPath) { zip.getInputStream(fileHeader) } }

                if (handler == null) {
                    // If no handler supports this file, throw an error
                    error("No handler found for file: $bookPath")
                } else {
                    // Use the handler to get the book info
                    val book = handler.bookInfo(bookPath) { zip.getInputStream(fileHeader) }
                    DelegatingBook(book).also { it.path = path }
                }
            }
        }
    }

    override fun getData(path: String, handlers: Collection<BookHandler>): InputStream {
        // Similar to obtainBook, we need to find the actual book file
        val components = parseInpxPath(path)
        val inpxPath = components.inpxPath + INPX_EXTENSION
        val inpFileName = components.inpFileName
        val bookPath = components.bookPath

        // Find the ZIP file next to the INPX file with the same name but .zip extension
        val inpxFile = File(inpxPath)
        val zipFileName = File(inpFileName).nameWithoutExtension
        val zipFile = File(inpxFile.parent, "$zipFileName.zip")

        if (!zipFile.exists()) {
            // If the ZIP file doesn't exist, throw an error
            error("ZIP file not found for INPX: $inpxPath")
        }

        // Open the ZIP file
        val zip = ZipFile(zipFile)

        try {
            val fileHeader = zip.fileHeaders.find { it.fileName == bookPath }

            if (fileHeader == null) {
                // If the file doesn't exist in the ZIP, throw an error
                zip.close()
                error("File not found in ZIP: $bookPath")
            }

            // Get the InputStream for the file and return it
            // This will keep the ZipFile open until the InputStream is closed
            return zip.getInputStream(fileHeader)
        } catch (e: Exception) {
            // If there's an error, close the ZipFile and throw an error
            zip.close()
            error("Error reading file from ZIP: $bookPath. ${e.message}")
        }
    }

    override fun obtainBookSize(path: String): Long {
        val (inpxPath, inpFileName, bookPath) = parseInpxPath(path)
        ZipFile(File(inpxPath).parentFile.resolve(File(inpFileName).nameWithoutExtension + ".zip")).use {
            return it.getFileHeader(bookPath)?.uncompressedSize ?: 0L
        }
    }
}

private data class InpxBook(
    override val title: String,
    override val authors: List<Author>,
    override val cover: ByteArray? = null,
    override val coverContentType: String? = null,
    override val annotation: String? = null,
    override val genres: List<String> = emptyList(),
    override val sequenceName: String? = null,
    override val sequenceNumber: Int? = null,
    override val path: String
) : Book {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InpxBook

        if (title != other.title) return false
        if (authors != other.authors) return false
        if (cover != null) {
            if (other.cover == null) return false
            if (!cover.contentEquals(other.cover)) return false
        } else if (other.cover != null) return false
        if (coverContentType != other.coverContentType) return false
        if (annotation != other.annotation) return false
        if (genres != other.genres) return false
        if (sequenceName != other.sequenceName) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + authors.hashCode()
        result = 31 * result + (cover?.contentHashCode() ?: 0)
        result = 31 * result + (coverContentType?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + genres.hashCode()
        result = 31 * result + (sequenceName?.hashCode() ?: 0)
        result = 31 * result + (sequenceNumber ?: 0)
        result = 31 * result + path.hashCode()
        return result
    }
}
