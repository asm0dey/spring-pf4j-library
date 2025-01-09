package com.github.asm0dey.opdsko_spring

import com.meilisearch.sdk.Client
import com.meilisearch.sdk.SearchRequest
import com.meilisearch.sdk.json.GsonJsonHandler
import generated.jooq.tables.daos.BookDao
import generated.jooq.tables.pojos.Author
import generated.jooq.tables.pojos.Book
import generated.jooq.tables.pojos.Genre
import generated.jooq.tables.records.BookRecord
import generated.jooq.tables.references.BOOK
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import org.jooq.SortField
import org.jooq.impl.DSL.*
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
@Lazy(false)
class BookRepo(val dao: BookDao, val meilisearch: Client) {

    private fun bookInfoCTE(vararg orderFields: SortField<*>?) =
        name("bookWithInfo").`as`(
            selectDistinct(
                BOOK.ID,
                BOOK,
                multiset(BOOK.author).`as`("_authors").convertFrom { it.into(Author::class.java) },
                multiset(BOOK.genre).`as`("_genres").convertFrom { it.into(Genre::class.java) },
                BOOK.SEQUENCE
            )
                .from(BOOK)
                .orderBy(*orderFields)
        )

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
        val bookInfoCTE = bookInfoCTE()
        return with(bookInfoCTE)
            .select(bookInfoCTE)
            .from(bookInfoCTE)
            .where(bookInfoCTE.field("id")?.cast(Long::class.java)?.eq(any(*ids.keys.toTypedArray())))
//            .orderBy(bookInfoCTE.field("id")?.cast(Long::class.java)?.sortAsc(ids))
            .fetchAsync()
            .await()
            .into(BookWithInfo::class.java)
            .sortedWith { a, b -> ids[a.id]!!.compareTo(ids[b.id]!!) }
            .toList()
    }

    @Transactional
    suspend fun save(toList: List<Book>) {
        using(dao.configuration()).batchInsert(toList.map { BookRecord(it) }).executeAsync().await()
    }

    suspend fun newBooks(page: Int): List<BookWithInfo> {
        val bookInfoCTE = bookInfoCTE(BOOK.ADDED.desc())
        return with(bookInfoCTE)
            .select(bookInfoCTE)
            .from(bookInfoCTE)
            .limit(50).offset(page * 50)
            .fetchAsync()
            .await()
            .into(BookWithInfo::class.java)
            .toList()
    }


}