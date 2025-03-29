package com.github.asm0dey.opdsko.inpx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InpxBookHandlerAuthorsTest {

    private val handler = InpxBookHandler()

    @Test
    fun `test parseAuthors with empty string`() {
        val authors = handler.parseAuthors("")
        assertTrue(authors.isEmpty())
    }

    @Test
    fun `test parseAuthors with blank string`() {
        val authors = handler.parseAuthors("   ")
        assertTrue(authors.isEmpty())
    }

    @Test
    fun `test parseAuthors with single author`() {
        val authors = handler.parseAuthors("Doe, John")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals(null, authors[0].middleName)
    }

    @Test
    fun `test parseAuthors with single author and comma`() {
        val authors = handler.parseAuthors("Doe, John")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals(null, authors[0].middleName)
    }

    @Test
    fun `test parseAuthors with single author and middle name`() {
        val authors = handler.parseAuthors("Doe, John, Smith")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals("Smith", authors[0].middleName)
    }

    @Test
    fun `test parseAuthors with multiple authors`() {
        val authors = handler.parseAuthors("Doe, John:Smith, Jane")
        assertEquals(2, authors.size)

        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals(null, authors[0].middleName)

        assertEquals("Smith", authors[1].lastName)
        assertEquals("Jane", authors[1].firstName)
        assertEquals(null, authors[1].middleName)
    }

    @Test
    fun `test parseAuthors with multiple authors and commas`() {
        val authors = handler.parseAuthors("Doe, John:Smith, Jane")
        assertEquals(2, authors.size)

        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals(null, authors[0].middleName)

        assertEquals("Smith", authors[1].lastName)
        assertEquals("Jane", authors[1].firstName)
        assertEquals(null, authors[1].middleName)
    }

    @Test
    fun `test parseAuthors with complex names`() {
        val authors = handler.parseAuthors("van, Doe, John, Smith:de, la, Smith, Jane, Marie")
        assertEquals(2, authors.size)

        assertEquals("van", authors[0].lastName)
        assertEquals("Doe", authors[0].firstName)
        assertEquals("John, Smith", authors[0].middleName)

        assertEquals("de", authors[1].lastName)
        assertEquals("la", authors[1].firstName)
        assertEquals("Smith, Jane, Marie", authors[1].middleName)
    }

    @Test
    fun `test parseAuthors with trailing colon`() {
        val authors = handler.parseAuthors("Doe, John:")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals(null, authors[0].middleName)
    }

    @Test
    fun `test parseAuthors with multiple name parts separated by comma`() {
        val authors = handler.parseAuthors("Doe, John, Smith")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals("Smith", authors[0].middleName)
    }

    @Test
    fun `test parseAuthors with multiple authors and multiple name parts`() {
        val authors = handler.parseAuthors("Doe, John, Smith:Johnson, Jane, Marie")
        assertEquals(2, authors.size)

        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals("Smith", authors[0].middleName)

        assertEquals("Johnson", authors[1].lastName)
        assertEquals("Jane", authors[1].firstName)
        assertEquals("Marie", authors[1].middleName)
    }

    @Test
    fun `test parseAuthors with multiple authors and multiple name parts ending with comma`() {
        val authors = handler.parseAuthors("Doe, John, Smith:Johnson, Jane, Marie:")
        assertEquals(2, authors.size)

        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals("Smith", authors[0].middleName)

        assertEquals("Johnson", authors[1].lastName)
        assertEquals("Jane", authors[1].firstName)
        assertEquals("Marie", authors[1].middleName)
    }

    @Test
    fun `test parseAuthors with more than three name parts`() {
        val authors = handler.parseAuthors("Doe, John, Smith, Jr, III")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals("Smith, Jr, III", authors[0].middleName)
    }

    @Test
    fun `test parseAuthors with trailing colon and comma-separated name`() {
        val authors = handler.parseAuthors("Doe, John:")
        assertEquals(1, authors.size)
        assertEquals("Doe", authors[0].lastName)
        assertEquals("John", authors[0].firstName)
        assertEquals(null, authors[0].middleName)
    }
}
