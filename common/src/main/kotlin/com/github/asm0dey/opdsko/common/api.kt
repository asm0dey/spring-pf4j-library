package com.github.asm0dey.opdsko.common

import org.pf4j.ExtensionPoint
import java.io.File
import java.io.InputStream

interface BookHandler : ExtensionPoint {
    fun supportsFile(fileName: String, data: () -> InputStream): Boolean
    fun bookInfo(fileName: String, dataProvider: () -> InputStream): Book
}

interface DelegatingBookHandler : ExtensionPoint {
    fun supportFile(file: File): Boolean
    fun bookProvider(file: File): Sequence<Pair<String, () -> InputStream>>
}