package com.github.asm0dey.opdsko.converter.fb2toepub

import com.github.asm0dey.opdsko.common.FormatConverter
import org.pf4j.Extension
import java.io.File
import java.io.IOException

@Extension(points = [FormatConverter::class])
class Fb2ToEpubConverter : FormatConverter {
    override val sourceFormat: String = "fb2"
    override val targetFormat: String = "epub"

    override fun canConvert(file: File): Boolean {
        return file.exists() && file.isFile && file.extension.equals("fb2", ignoreCase = true) && Fb2ToEpubConverterPlugin.epubConverterAccessible
    }

    override fun convert(sourceFile: File, targetFile: File?): File {
        if (!canConvert(sourceFile)) {
            throw IllegalArgumentException("Cannot convert file: ${sourceFile.absolutePath}")
        }

        val outputFile = targetFile ?: File(sourceFile.parent, "${sourceFile.nameWithoutExtension}.epub")
        
        val process = ProcessBuilder(
            "./fb2c",
            sourceFile.absolutePath,
            outputFile.absolutePath
        ).redirectErrorStream(true).start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val errorOutput = process.inputStream.bufferedReader().readText()
            throw IOException("Conversion failed with exit code $exitCode: $errorOutput")
        }

        return outputFile
    }
}