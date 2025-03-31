package com.github.asm0dey.opdsko_spring.repo

import com.github.asm0dey.opdsko_spring.Book
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

data class PagedBooks(val books: List<Book>, val total: Long)


@Repository
@Transactional(readOnly = true)
@Lazy(false)
class BookRepo(
    private val meilisearch: Client,
    private val bookMongoRepository: BookMongoRepository,
) {

    suspend fun searchBookByName(searchTerm: String, page: Int): List<Book> {
        val rawHits = meilisearch.index("books")
            .search(
                SearchRequest(searchTerm)
                    .apply {
                        limit = 15
                        offset = 15 * page
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
    suspend fun save(toList: List<Book>): List<Book> {
        return bookMongoRepository.saveAll(toList).toList()
    }

    suspend fun newBooks(page: Int): PagedBooks {
        val books = bookMongoRepository
            .findAllBy(PageRequest.of(page, 24, Sort.by(Sort.Direction.DESC, "added")))
            .toList()
        val total = bookMongoRepository.count()
        return PagedBooks(books, total)
    }
}
