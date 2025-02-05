package com.github.asm0dey.opdsko_spring

import com.meilisearch.sdk.Client
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Repository


@Serializable
data class BookIndexItem(val id: String, val name: String)

@Repository
class Meilisearch(val client: Client) {
    fun saveBook(d: BookIndexItem) {
        client.index("books")
            .addDocuments(Json.encodeToString(d), "id")
    }

    fun saveBooks(books: List<BookIndexItem>) {
        client.index("books")
            .addDocuments(Json.encodeToString(books), "id")
    }

    fun search(query: String) =
        client.index("books").search(query).hits.map { BookIndexItem(it["id"] as String, it["name"] as String) }
}