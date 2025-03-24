package com.github.asm0dey.opdsko_spring

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.SearchRequest
import com.meilisearch.sdk.json.GsonJsonHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
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
    private val meilisearch: Client,
    private val bookMongoRepository: BookMongoRepository,
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
    suspend fun save(toList: List<Book>): Flow<Book> {
        val existingBookPaths = bookMongoRepository.findAllByPathIn(toList.map(Book::path)).map { it.path }.toSet()
        return bookMongoRepository.saveAll(toList.filterNot { it.path in existingBookPaths })
    }

    suspend fun newBooks(page: Int): List<BookWithInfo> = bookMongoRepository
        .findAllBy(PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "added")))
        .toList()
}
