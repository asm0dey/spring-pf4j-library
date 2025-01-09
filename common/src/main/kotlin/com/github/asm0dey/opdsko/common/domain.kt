package com.github.asm0dey.opdsko.common

interface Book {
    val title: String
    val cover: ByteArray?
    val coverContentType: String?
    val annotation: String?
    val authors: List<Author>
    val genres: List<String>
    val sequenceName: String?
    val sequenceNumber: Int?
    val path: String
}

interface Author {
    val firstName: String?
    val lastName: String?
    val middleName: String?
    val nickname: String?
    fun fullName(): String = buildString {
        if (lastName != null) append(lastName)
        val hasLastName = lastName != null
        if (firstName != null) {
            if (hasLastName) append(", ")
            append(firstName).append(" ")
        }
        if (middleName != null) {
            if (firstName == null && hasLastName) append(", ")
            append(middleName).append(" ")
        }
        if (nickname != null) {
            when {
                lastName == null && firstName == null && middleName == null -> append(nickname)
                lastName != null && firstName == null && middleName == null -> append(" ").append(nickname)
                else -> append("($nickname)")
            }
        }

    }.trim()
}

class AuthorAdapter(
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val middleName: String? = null,
    override val nickname: String? = null,
) : Author

fun main() {
    println(AuthorAdapter(lastName = "Fin").fullName())
    println(AuthorAdapter(firstName = "Pas").fullName())
    println(AuthorAdapter(middleName = "Mik").fullName())
    println(AuthorAdapter(nickname = "asm0dey").fullName())
    println(AuthorAdapter(lastName = "Fin", nickname = "asm0dey").fullName())
    println(AuthorAdapter(firstName = "Pas", nickname = "asm0dey").fullName())
    println(AuthorAdapter(middleName = "Mik", nickname = "asm0dey").fullName())
    println(AuthorAdapter(lastName = "Fin", middleName = "Mik", firstName = "Pas").fullName())
    println(AuthorAdapter(lastName = "Fin", middleName = "Mik", firstName = "Pas", nickname = "asm0dey").fullName())
    println(AuthorAdapter(lastName = "Fin", firstName = "Pas").fullName())
    println(AuthorAdapter(lastName = "Fin", firstName = "Pas Mik").fullName())
}
