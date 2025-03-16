package com.github.asm0dey.opdsko_spring

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.SearchRequest
import com.meilisearch.sdk.json.GsonJsonHandler
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional


@Repository
@Transactional(readOnly = true)
@Lazy(false)
class BookRepo(
    val meilisearch: Client,
    val bookMongoRepository: BookMongoRepository,
) {

    suspend fun searchBookByText(searchTerm: String, page: Int): List<BookWithInfo> {
        val rawHits = meilisearch.index("books")
            .search(
                SearchRequest(searchTerm)
                    .apply {
                        limit = 50
                        offset = 50 * page
                    })
            .hits
        val ids = Json.decodeFromString<List<BookIndexItem>>(GsonJsonHandler().encode(rawHits))
            .map(BookIndexItem::id)
            .mapIndexed { index, id -> id to index }
            .toMap()
        return bookMongoRepository
            .findAllByIdIn(ids.keys.toList())
            .toList()
            .sortedWith { a, b -> ids[a.id]!!.compareTo(ids[b.id]!!) }
    }

    @Transactional
    suspend fun save(toList: List<Book>) = bookMongoRepository.saveAll(toList)

    suspend fun newBooks(page: Int): List<BookWithInfo> = bookMongoRepository
        .findAllBy(PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "added")))
        .toList()
}