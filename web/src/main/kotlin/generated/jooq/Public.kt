/*
 * This file is generated by jOOQ.
 */
package generated.jooq


import generated.jooq.tables.Author
import generated.jooq.tables.Book
import generated.jooq.tables.BookAuthor
import generated.jooq.tables.BookGenre
import generated.jooq.tables.FindAuthorByNames
import generated.jooq.tables.Genre
import generated.jooq.tables.records.FindAuthorByNamesRecord

import javax.annotation.processing.Generated

import kotlin.collections.List

import org.jooq.Catalog
import org.jooq.Configuration
import org.jooq.Field
import org.jooq.Result
import org.jooq.Table
import org.jooq.impl.SchemaImpl


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = [
        "https://www.jooq.org",
        "jOOQ version:3.19.16",
        "schema version:0"
    ],
    comments = "This class is generated by jOOQ"
)
@Suppress("UNCHECKED_CAST")
open class Public : SchemaImpl("public", DefaultCatalog.DEFAULT_CATALOG) {
    companion object {

        /**
         * The reference instance of <code>public</code>
         */
        val PUBLIC: Public = Public()
    }

    /**
     * The table <code>public.author</code>.
     */
    val AUTHOR: Author get() = Author.AUTHOR

    /**
     * The table <code>public.book</code>.
     */
    val BOOK: Book get() = Book.BOOK

    /**
     * The table <code>public.book_author</code>.
     */
    val BOOK_AUTHOR: BookAuthor get() = BookAuthor.BOOK_AUTHOR

    /**
     * The table <code>public.book_genre</code>.
     */
    val BOOK_GENRE: BookGenre get() = BookGenre.BOOK_GENRE

    /**
     * The table <code>public.find_author_by_names</code>.
     */
    val FIND_AUTHOR_BY_NAMES: FindAuthorByNames get() = FindAuthorByNames.FIND_AUTHOR_BY_NAMES

    /**
     * Call <code>public.find_author_by_names</code>.
     */
    fun FIND_AUTHOR_BY_NAMES(
          configuration: Configuration
        , iFirstName: String?
        , iMiddleName: String?
        , iLastName: String?
        , iNickname: String?
    ): Result<FindAuthorByNamesRecord> = configuration.dsl().selectFrom(generated.jooq.tables.FindAuthorByNames.FIND_AUTHOR_BY_NAMES.call(
          iFirstName
        , iMiddleName
        , iLastName
        , iNickname
    )).fetch()

    /**
     * Get <code>public.find_author_by_names</code> as a table.
     */
    fun FIND_AUTHOR_BY_NAMES(
          iFirstName: String?
        , iMiddleName: String?
        , iLastName: String?
        , iNickname: String?
    ): FindAuthorByNames = generated.jooq.tables.FindAuthorByNames.FIND_AUTHOR_BY_NAMES.call(
        iFirstName,
        iMiddleName,
        iLastName,
        iNickname
    )

    /**
     * Get <code>public.find_author_by_names</code> as a table.
     */
    fun FIND_AUTHOR_BY_NAMES(
          iFirstName: Field<String?>
        , iMiddleName: Field<String?>
        , iLastName: Field<String?>
        , iNickname: Field<String?>
    ): FindAuthorByNames = generated.jooq.tables.FindAuthorByNames.FIND_AUTHOR_BY_NAMES.call(
        iFirstName,
        iMiddleName,
        iLastName,
        iNickname
    )

    /**
     * The table <code>public.genre</code>.
     */
    val GENRE: Genre get() = Genre.GENRE

    override fun getCatalog(): Catalog = DefaultCatalog.DEFAULT_CATALOG

    override fun getTables(): List<Table<*>> = listOf(
        Author.AUTHOR,
        Book.BOOK,
        BookAuthor.BOOK_AUTHOR,
        BookGenre.BOOK_GENRE,
        FindAuthorByNames.FIND_AUTHOR_BY_NAMES,
        Genre.GENRE
    )
}
