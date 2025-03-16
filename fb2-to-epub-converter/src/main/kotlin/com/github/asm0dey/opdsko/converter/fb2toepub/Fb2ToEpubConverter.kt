package com.github.asm0dey.opdsko.converter.fb2toepub

import com.github.asm0dey.opdsko.common.FormatConverter
import org.pf4j.Extension
import java.io.IOException
import java.io.InputStream
import kotlin.io.path.createTempFile

@Extension(points = [FormatConverter::class])
class Fb2ToEpubConverter : FormatConverter {
    override val sourceFormat: String = "fb2"
    override val targetFormat: String = "epub"

    override fun canConvert(sourceFormat: String): Boolean {
        return sourceFormat.equals("fb2", ignoreCase = true) && Fb2ToEpubConverterPlugin.epubConverterAccessible
    }

    override fun convert(inputStream: InputStream): InputStream {
        if (!canConvert(sourceFormat)) {
            throw IllegalArgumentException("Cannot convert from $sourceFormat to $targetFormat")
        }

        // Create temporary files for input and output
        val tempInputFile = createTempFile("fb2_input_", ".fb2").toFile()
        tempInputFile.deleteOnExit()

        val tempOutputFile = createTempFile("epub_output_", ".epub").toFile()
        tempOutputFile.deleteOnExit()

        try {
            // Write input stream to temporary file
            tempInputFile.outputStream().use { output ->
                inputStream.use { it.copyTo(output) }
            }

            // Run the conversion process
            val process = ProcessBuilder(
                "./fb2c",
                tempInputFile.absolutePath,
                tempOutputFile.absolutePath
            ).redirectErrorStream(true).start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val errorOutput = process.inputStream.bufferedReader().readText()
                throw IOException("Conversion failed with exit code $exitCode: $errorOutput")
            }

            // Return the output file as a stream
            return tempOutputFile.inputStream()
        } catch (e: Exception) {
            // Clean up temporary files in case of error
            tempInputFile.delete()
            tempOutputFile.delete()
            throw e
        } finally {
            tempInputFile.delete()
        }
    }
}
