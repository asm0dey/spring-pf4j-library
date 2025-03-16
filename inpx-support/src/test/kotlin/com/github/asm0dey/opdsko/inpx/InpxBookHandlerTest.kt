package com.github.asm0dey.opdsko.inpx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InpxBookHandlerTest {

    private val handler = InpxBookHandler()

    @Test
    fun `test supportsPath with valid path`() {
        val path = "inpx::/path/to/library.inpx#books.inp*book.fb2"
        assertTrue(handler.supportsPath(path))
    }

    @Test
    fun `test supportsPath with invalid path`() {
        val path1 = "zip::/path/to/library.zip#book.fb2"
        val path2 = "/path/to/book.fb2"
        val path3 = "inpx::/path/to/library.inpx"

        assertFalse(handler.supportsPath(path1))
        assertFalse(handler.supportsPath(path2))
        assertFalse(handler.supportsPath(path3))
    }

    @Test
    fun `test path parsing`() {
        val inpxPath = "/path/to/library"
        val inpFileName = "books.inp"
        val bookPath = "book.fb2"
        val fullPath = "inpx::$inpxPath.inpx#$inpFileName*$bookPath"

        // Extract the components using the parseInpxPath function
        val components = parseInpxPath(fullPath)

        // Verify the extracted components
        assertEquals(inpxPath, components.inpxPath)
        assertEquals(inpFileName, components.inpFileName)
        assertEquals(bookPath, components.bookPath)
    }

    @Test
    fun `test path construction`() {
        val inpxPath = "/path/to/library"
        val inpFileName = "books.inp"
        val bookPath = "book.fb2"
        val format = "fb2"

        // Construct the path using the createInpxPath function
        val constructedPath = createInpxPath(inpxPath, inpFileName, bookPath, format)

        // Verify the constructed path matches the expected format
        assertEquals("inpx::/path/to/library.inpx#books.inp*book.fb2.fb2", constructedPath)
    }
}
