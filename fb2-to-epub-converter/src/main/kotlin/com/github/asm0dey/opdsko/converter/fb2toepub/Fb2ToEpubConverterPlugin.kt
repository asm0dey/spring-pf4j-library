package com.github.asm0dey.opdsko.converter.fb2toepub

import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.attribute.PosixFilePermission.*
import kotlin.io.path.Path
import kotlin.io.path.setPosixFilePermissions

private const val FB2C_VERSION = "v1.77.1"

class Fb2ToEpubConverterPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    companion object {
        var epubConverterAccessible = false
        private val os by lazy {
            val osName = System.getProperty("os.name").lowercase()
            when {
                osName.contains("win") -> "win"
                osName.contains("linux") -> "linux"
                osName.contains("mac") -> "darwin"
                else -> {
                    epubConverterAccessible = false
                    null
                }
            }
        }
    }

    override fun start() {
        super.start()
        downloadEpubConverter()
    }

    private fun downloadEpubConverter() {
        val osArch = System.getProperty("os.arch").lowercase()
        val arch =
            if (os != null) {
                when {
                    osArch.contains("64") ->
                        when (os) {
                            "linux", "win", "darwin" -> "amd64"
                            else -> error("Unsupported platform")
                        }

                    osArch.contains("86") ->
                        when (os) {
                            "linux", "win" -> "386"
                            else -> error("Unsupported platform")
                        }

                    else -> {
                        epubConverterAccessible = false
                        null
                    }
                }
            } else null
        if (arch != null) {
            val targetFile = "fb2c-$FB2C_VERSION.zip"
            if (!File(targetFile).exists()) {
                log.info(
                    "It seems that there is no archive of fb2c next to the executable, downloading it…"
                )
                downloadFile(
                    URI(
                        "https://github.com/rupor-github/fb2converter/releases/download/$FB2C_VERSION/fb2c-$os-$arch.zip"
                    )
                        .toURL(),
                    targetFile
                )
                log.info(
                    """Downloaded fb2c archive.
                |Unpacking…
            """.trimMargin()
                )
            }
            net.lingala.zip4j.ZipFile(targetFile).use {
                val myHeader = it.fileHeaders.first { it.fileName.startsWith("fb2c") }
                it.extractFile(
                    myHeader,
                    it.file.absoluteFile.parentFile.absolutePath,
                    myHeader.fileName.substringAfter('/')
                )
                posixSetAccessible(myHeader.fileName)
            }
            setConfig()
            log.info("Unpacked. Resuming application launch.")
            epubConverterAccessible = true
        }
    }

    private fun setConfig() {
        if (!File("fb2c.conf.toml").exists())
            this::class
                .java
                .classLoader
                .getResourceAsStream("fb2c.conf.toml")
                ?.buffered()
                ?.use { inp ->
                    FileOutputStream("fb2c.conf.toml").buffered().use { out ->
                        inp.copyTo(out)
                    }
                    posixSetAccessible("fb2c.conf.toml")
                }
    }

    private fun downloadFile(url: URL, outputFileName: String) {
        Channels.newChannel(url.openStream()).use { rbc ->
            FileOutputStream(outputFileName).use { fos ->
                fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
            }
        }
    }

    private fun posixSetAccessible(fileName: String) =
        try {
            Path(fileName).setPosixFilePermissions(setOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE))
        } catch (_: Exception) {
        }
}