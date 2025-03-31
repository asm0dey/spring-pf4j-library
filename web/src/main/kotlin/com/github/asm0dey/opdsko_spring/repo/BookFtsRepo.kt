package com.github.asm0dey.opdsko_spring.repo

import com.meilisearch.sdk.Client
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.data.annotation.Id
import org.springframework.stereotype.Repository


@Serializable
data class BookIndexItem(@Id val id: String, val name: String)

@Repository
class Meilisearch(val client: Client) {

    fun saveBooks(books: List<BookIndexItem>) {
        client.index("books")
            .addDocuments(Json.encodeToString(books), "id")
    }

    /**
     * Deletes multiple books from the Meilisearch index.
     * 
     * @param ids The IDs of the books to delete
     */
    fun deleteBooks(ids: List<String>) {
        client.index("books").deleteDocuments(ids)
    }
    fun deleteAllBooks() {
        client.index("books").deleteAllDocuments()
    }

    fun search(query: String) =
        client.index("books").search(query).hits.map { BookIndexItem(it["id"] as String, it["name"] as String) }
}
