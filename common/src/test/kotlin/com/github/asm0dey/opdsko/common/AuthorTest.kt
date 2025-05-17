package com.github.asm0dey.opdsko.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthorTest {

    @Test
    fun `computeFullName with lastName only`() {
        val author = AuthorAdapter(lastName = "Doe")
        assertEquals("Doe", author.computeFullName())
    }

    @Test
    fun `computeFullName with firstName only`() {
        val author = AuthorAdapter(firstName = "John")
        assertEquals("John", author.computeFullName())
    }

    @Test
    fun `computeFullName with nickname only`() {
        val author = AuthorAdapter(nickname = "JD")
        assertEquals("JD", author.computeFullName())
    }

    @Test
    fun `computeFullName with lastName and firstName`() {
        val author = AuthorAdapter(lastName = "Doe", firstName = "John")
        assertEquals("Doe, John", author.computeFullName())
    }

    @Test
    fun `computeFullName with lastName, firstName, and nickname`() {
        val author = AuthorAdapter(lastName = "Doe", firstName = "John", nickname = "JD")
        assertEquals("Doe, John (JD)", author.computeFullName())
    }

    @Test
    fun `computeFullName with all fields`() {
        val author = AuthorAdapter(lastName = "Doe", firstName = "John", middleName = "Smith", nickname = "JD")
        assertEquals("Doe, John Smith (JD)", author.computeFullName())
    }
}