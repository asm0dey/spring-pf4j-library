package com.github.asm0dey.opdsko_spring

import generated.jooq.routines.references.similarity
import generated.jooq.tables.daos.BookDao
import generated.jooq.tables.pojos.Author
import generated.jooq.tables.pojos.Book
import generated.jooq.tables.pojos.Genre
import generated.jooq.tables.records.BookRecord
import generated.jooq.tables.references.BOOK
import kotlinx.coroutines.future.await
import org.jooq.SortField
import org.jooq.impl.DSL.*
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
@Lazy(false)
class BookRepo(val dao: BookDao) {

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
        val sml = similarity(BOOK.NAME, `val`(searchTerm))
        val ids =
            name("ids").fields("id1", "sml").`as`(
                select(BOOK.ID, sml.`as`("sml"))
                    .from(BOOK)
                    .where(sml.ge(.3f))
                    .orderBy(field("sml").desc()).limit(50).offset(page * 50)
            )
        val bookInfoCTE = bookInfoCTE()
        return with(ids)
            .with(bookInfoCTE)
            .select(bookInfoCTE)
            .from(bookInfoCTE)
            .innerJoin(ids).on(ids.field("id1", Long::class.java)!!.eq(bookInfoCTE.field(BOOK.ID)))
            .fetchAsync()
            .await()
            .into(BookWithInfo::class.java)
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