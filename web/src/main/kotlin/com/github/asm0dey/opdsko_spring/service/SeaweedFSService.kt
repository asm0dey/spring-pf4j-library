package com.github.asm0dey.opdsko_spring.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import seaweedfs.client.FilerClient
import seaweedfs.client.SeaweedInputStream
import seaweedfs.client.SeaweedOutputStream
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

@Service
class SeaweedFSService(private val filerClient: FilerClient) {
    private val logger = LoggerFactory.getLogger(SeaweedFSService::class.java)

    // Maximum dimensions for preview images
    private val MAX_PREVIEW_WIDTH = 200
    private val MAX_PREVIEW_HEIGHT = 300

    /**
     * Saves a book description to SeaweedFS.
     *
     * @param bookId The ID of the book
     * @param description The book description text
     * @return true if the description was saved successfully, false otherwise
     */
    fun saveBookDescription(bookId: String, description: String): Boolean {
        if (description.isEmpty()) {
            return false
        }

        try {
            SeaweedOutputStream(filerClient, "/descriptions/$bookId").use { outputStream ->
                outputStream.write(description.toByteArray())
            }
            return true
        } catch (e: Exception) {
            logger.error("Error saving book description to SeaweedFS: ${e.message}", e)
            return false
        }
    }

    /**
     * Retrieves a book description from SeaweedFS.
     *
     * @param bookId The ID of the book
     * @return The description text, or null if not found
     */
    fun getBookDescription(bookId: String): String? {
        try {
            return SeaweedInputStream(filerClient, "/descriptions/$bookId").use { inputStream ->
                String(inputStream.readAllBytes())
            }
        } catch (e: Exception) {
            logger.warn("Error retrieving book description from SeaweedFS: ${e.message}")
            return null
        }
    }

    /**
     * Resizes an image to fit within the maximum dimensions while preserving aspect ratio.
     *
     * @param imageData The original image data
     * @param contentType The content type of the image
     * @return The resized image data, or null if resizing failed
     */
    private fun createPreviewImage(imageData: ByteArray, contentType: String): ByteArray? {
        try {
            // Read the image
            val inputStream = ByteArrayInputStream(imageData)
            val originalImage = ImageIO.read(inputStream)

            if (originalImage == null) {
                logger.error("Failed to read image data")
                return null
            }

            // Calculate new dimensions while preserving aspect ratio
            val originalWidth = originalImage.width
            val originalHeight = originalImage.height

            // If image is already smaller than max dimensions, return original
            if (originalWidth <= MAX_PREVIEW_WIDTH && originalHeight <= MAX_PREVIEW_HEIGHT) {
                return imageData
            }

            // Calculate scaling factor
            val widthRatio = MAX_PREVIEW_WIDTH.toDouble() / originalWidth
            val heightRatio = MAX_PREVIEW_HEIGHT.toDouble() / originalHeight
            val scaleFactor = minOf(widthRatio, heightRatio)

            // Calculate new dimensions
            val newWidth = (originalWidth * scaleFactor).toInt()
            val newHeight = (originalHeight * scaleFactor).toInt()

            // Create resized image
            val resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
            val bufferedResizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)

            // Draw the resized image onto the buffered image
            val graphics = bufferedResizedImage.createGraphics()
            graphics.drawImage(resizedImage, 0, 0, null)
            graphics.dispose()

            // Convert to byte array
            val outputStream = ByteArrayOutputStream()
            val formatName = when (contentType.lowercase()) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                else -> "jpg" // Default to JPEG
            }

            ImageIO.write(bufferedResizedImage, formatName, outputStream)
            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Error creating preview image: ${e.message}", e)
            return null
        }
    }

    /**
     * Saves a book cover to SeaweedFS.
     * Also creates and saves a preview image with dimensions not exceeding 200x300.
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
            // Save original image
            SeaweedOutputStream(filerClient, "/covers/$bookId").use { outputStream ->
                outputStream.write(coverData)
            }
            SeaweedOutputStream(filerClient, "/covers/$bookId.metadata").use { outputStream ->
                outputStream.write(contentType.toByteArray())
            }

            // Create and save preview image
            val previewData = createPreviewImage(coverData, contentType)
            if (previewData != null) {
                SeaweedOutputStream(filerClient, "/covers/$bookId.preview").use { outputStream ->
                    outputStream.write(previewData)
                }
                // Preview uses the same content type as the original
                SeaweedOutputStream(filerClient, "/covers/$bookId.preview.metadata").use { outputStream ->
                    outputStream.write(contentType.toByteArray())
                }
            } else {
                logger.warn("Failed to create preview for book cover $bookId, using original as preview")
                // If preview creation fails, use the original as the preview
                SeaweedOutputStream(filerClient, "/covers/$bookId.preview").use { outputStream ->
                    outputStream.write(coverData)
                }
                SeaweedOutputStream(filerClient, "/covers/$bookId.preview.metadata").use { outputStream ->
                    outputStream.write(contentType.toByteArray())
                }
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
            logger.warn("Error retrieving book cover from SeaweedFS: ${e.message}")
            return null
        }
    }

    /**
     * Retrieves a book cover preview from SeaweedFS.
     * If the preview doesn't exist, it returns null.
     *
     * @param bookId The ID of the book
     * @return The preview image data as an InputStream, or null if not found
     */
    fun getBookCoverPreview(bookId: String): InputStream? {
        try {
            return SeaweedInputStream(filerClient, "/covers/$bookId.preview")
        } catch (e: Exception) {
            logger.warn("Error retrieving book cover preview from SeaweedFS: ${e.message}")
            return null
        }
    }

    /**
     * Retrieves the content type of a book cover from SeaweedFS.
     *
     * @param bookId The ID of the book
     * @return The content type of the cover image, or "application/octet-stream" if not found
     */
    fun getBookCoverContentType(bookId: String): String? {
        try {
            return SeaweedInputStream(filerClient, "/covers/$bookId.metadata").use { inputStream ->
                String(inputStream.readAllBytes())
            }
        } catch (e: Exception) {
            logger.warn("Error retrieving book cover content type from SeaweedFS for book $bookId")
            return null
        }
    }

    /**
     * Retrieves the content type of a book cover preview from SeaweedFS.
     * Since the preview uses the same content type as the original, this is the same as getBookCoverContentType.
     *
     * @param bookId The ID of the book
     * @return The content type of the preview image, or "application/octet-stream" if not found
     */
    fun getBookCoverPreviewContentType(bookId: String): String {
        try {
            return SeaweedInputStream(filerClient, "/covers/$bookId.preview.metadata").use { inputStream ->
                String(inputStream.readAllBytes())
            }
        } catch (e: Exception) {
            logger.error("Error retrieving book cover preview content type from SeaweedFS: ${e.message}", e)
            return "application/octet-stream"
        }
    }

    /**
     * Deletes a book cover, its preview, and their metadata from SeaweedFS.
     *
     * @param bookId The ID of the book
     * @return true if the cover was deleted successfully, false otherwise
     */
    fun deleteBookCover(bookId: String): Boolean {
        try {
            val path = "/covers/$bookId"
            val metadataPath = "/covers/$bookId.metadata"
            val previewPath = "/covers/$bookId.preview"
            val previewMetadataPath = "/covers/$bookId.preview.metadata"

            // Delete original image and metadata
            filerClient.rm(path, false, true)
            filerClient.rm(metadataPath, false, true)

            // Delete preview image and metadata
            try {
                filerClient.rm(previewPath, false, true)
                filerClient.rm(previewMetadataPath, false, true)
            } catch (e: Exception) {
                // If preview deletion fails, log but don't fail the whole operation
                logger.warn("Error deleting book cover preview from SeaweedFS: ${e.message}", e)
            }

            return true
        } catch (e: Exception) {
            logger.error("Error deleting book cover from SeaweedFS: ${e.message}", e)
            return false
        }
    }
}
