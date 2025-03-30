package com.github.asm0dey.opdsko.inpx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InpxBookHandlerGenresTest {

    private val handler = InpxBookHandler()

    @Test
    fun `test parseGenres with empty string`() {
        val genres = handler.parseGenres("")
        assertTrue(genres.isEmpty())
    }

    @Test
    fun `test parseGenres with blank string`() {
        val genres = handler.parseGenres("   ")
        assertTrue(genres.isEmpty())
    }

    @Test
    fun `test parseGenres with single genre`() {
        val genres = handler.parseGenres("Fiction")
        assertEquals(1, genres.size)
        assertEquals("Fiction", genres[0])
    }

    @Test
    fun `test parseGenres with multiple genres`() {
        val genres = handler.parseGenres("Fiction:Fantasy:Adventure")
        assertEquals(3, genres.size)
        assertEquals("Fiction", genres[0])
        assertEquals("Fantasy", genres[1])
        assertEquals("Adventure", genres[2])
    }

    @Test
    fun `test parseGenres with spaces`() {
        val genres = handler.parseGenres("Fiction : Fantasy : Adventure")
        assertEquals(3, genres.size)
        assertEquals("Fiction", genres[0])
        assertEquals("Fantasy", genres[1])
        assertEquals("Adventure", genres[2])
    }

    @Test
    fun `test parseGenres with empty genres`() {
        val genres = handler.parseGenres("Fiction::Adventure")
        assertEquals(2, genres.size)
        assertEquals("Fiction", genres[0])
        assertEquals("Adventure", genres[1])
    }
}