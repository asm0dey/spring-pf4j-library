package com.github.asm0dey.opdsko_spring

import com.meilisearch.sdk.Client
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
@ConfigurationProperties(prefix = "meilisearch")
class MeilisearchProperties {
    var apiKey: String? = null
    var host: String = ""
}

@Serializable
data class BookIndexItem(val id: Long, val name: String)

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
        client.index("books").search(query).hits.map { BookIndexItem(it["id"] as Long, it["name"] as String) }
}