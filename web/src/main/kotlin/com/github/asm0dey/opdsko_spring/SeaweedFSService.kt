package com.github.asm0dey.opdsko_spring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import seaweedfs.client.FilerClient
import seaweedfs.client.SeaweedInputStream
import seaweedfs.client.SeaweedOutputStream
import java.io.InputStream

@Service
class SeaweedFSService(private val filerClient: FilerClient) {
    private val logger = LoggerFactory.getLogger(SeaweedFSService::class.java)

    /**
     * Saves a book cover to SeaweedFS.
     *
     * @param bookId The ID of the book
     * @param coverData The cover image data
     * @param contentType The content type of the cover image
     * @return true if the cover was saved successfully, false otherwise
     */
    fun saveBookCover(bookId: String, coverData: ByteArray, contentType: String): Boolean {
        if (coverData.isEmpty()) {
            return false
        }

        try {
            SeaweedOutputStream(filerClient, "/covers/$bookId").use { outputStream ->
                outputStream.write(coverData)
            }
            SeaweedOutputStream(filerClient, "/covers/$bookId.metadata").use { outputStream ->
                outputStream.write(contentType.toByteArray())
            }

            return true
        } catch (e: Exception) {
            logger.error("Error saving book cover to SeaweedFS: ${e.message}", e)
            return false
        }
    }

    /**
     * Retrieves a book cover from SeaweedFS.
     *
     * @param bookId The ID of the book
     * @return The cover image data as an InputStream, or null if not found
     */
    fun getBookCover(bookId: String): InputStream? {
        try {
            return SeaweedInputStream(filerClient, "/covers/$bookId")
        } catch (e: Exception) {
            logger.error("Error retrieving book cover from SeaweedFS: ${e.message}", e)
            return null
        }
    }

    /**
     * Retrieves the content type of a book cover from SeaweedFS.
     *
     * @param bookId The ID of the book
     * @return The content type of the cover image, or "application/octet-stream" if not found
     */
    fun getBookCoverContentType(bookId: String): String {
        try {
            return SeaweedInputStream(filerClient, "/covers/$bookId.metadata").use { inputStream ->
                String(inputStream.readAllBytes())
            }
        } catch (e: Exception) {
            logger.error("Error retrieving book cover content type from SeaweedFS: ${e.message}", e)
            return "application/octet-stream"
        }
    }

    /**
     * Deletes a book cover and its metadata from SeaweedFS.
     *
     * @param bookId The ID of the book
     * @return true if the cover was deleted successfully, false otherwise
     */
    fun deleteBookCover(bookId: String): Boolean {
        try {
            val path = "/covers/$bookId"
            val metadataPath = "/covers/$bookId.metadata"
            filerClient.rm(path, false, true)
            filerClient.rm(metadataPath, false, true)

            return true
        } catch (e: Exception) {
            logger.error("Error deleting book cover from SeaweedFS: ${e.message}", e)
            return false
        }
    }
}
