package com.github.asm0dey.opdsko_spring

import com.github.asm0dey.opdsko.common.DelegatingBookHandler
import net.lingala.zip4j.ZipFile
import org.pf4j.Extension
import java.io.File
import java.io.InputStream

@Extension(points = [DelegatingBookHandler::class])
data object ZipBookHandler : DelegatingBookHandler {

    override fun supportFile(file: File) = file.name.endsWith(".zip")
    override fun bookProvider(file: File): Sequence<Pair<String, () -> InputStream>> {
        return ZipFile(file).use {
            sequence {
                for (fileHeader in it.fileHeaders) {
                    yield(fileHeader.fileName to { it.getInputStream(fileHeader) })
                }
            }
        }
    }

}