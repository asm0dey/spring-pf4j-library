package com.github.asm0dey.opdsko_spring.service

import org.springframework.core.io.AbstractResource
import java.io.File
import java.io.InputStream

/**
 * Data class to represent book cover data
 */
data class BookCoverData(
    val inputStream: InputStream,
    val contentType: String
)

/**
 * Data class to represent book download data
 */
data class BookDownloadData(
    val resource: AbstractResource, // Can be InputStreamResource or FileSystemResource
    val fileName: String,
    val contentType: String,
    val tempFile: File? = null // For temporary files that need to be deleted after use
)

/**
 * Enum to represent the result of an operation
 */
enum class OperationResult {
    NOT_FOUND,
    BAD_REQUEST,
    SUCCESS
}

/**
 * Data class to represent the result of an operation with data
 */
data class OperationResultWithData<T>(
    val result: OperationResult,
    val data: T? = null,
    val errorMessage: String? = null
)