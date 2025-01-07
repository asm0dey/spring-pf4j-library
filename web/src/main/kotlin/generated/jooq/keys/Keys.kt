/*
 * This file is generated by jOOQ.
 */
package generated.jooq.keys


import generated.jooq.tables.Author
import generated.jooq.tables.Book
import generated.jooq.tables.BookAuthor
import generated.jooq.tables.BookGenre
import generated.jooq.tables.Genre
import generated.jooq.tables.records.AuthorRecord
import generated.jooq.tables.records.BookAuthorRecord
import generated.jooq.tables.records.BookGenreRecord
import generated.jooq.tables.records.BookRecord
import generated.jooq.tables.records.GenreRecord

import org.jooq.ForeignKey
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal



// -------------------------------------------------------------------------
// UNIQUE and PRIMARY KEY definitions
// -------------------------------------------------------------------------

val AUTHOR_FULL_NAME_KEY: UniqueKey<AuthorRecord> = Internal.createUniqueKey(Author.AUTHOR, DSL.name("author_full_name_key"), arrayOf(Author.AUTHOR.FULL_NAME), true)
val AUTHOR_PKEY: UniqueKey<AuthorRecord> = Internal.createUniqueKey(Author.AUTHOR, DSL.name("author_pkey"), arrayOf(Author.AUTHOR.ID), true)
val BOOK_PKEY: UniqueKey<BookRecord> = Internal.createUniqueKey(Book.BOOK, DSL.name("book_pkey"), arrayOf(Book.BOOK.ID), true)
val BOOK_AUTHOR_PKEY: UniqueKey<BookAuthorRecord> = Internal.createUniqueKey(BookAuthor.BOOK_AUTHOR, DSL.name("book_author_pkey"), arrayOf(BookAuthor.BOOK_AUTHOR.BOOK_ID, BookAuthor.BOOK_AUTHOR.AUTHOR_ID), true)
val BOOK_GENRE_PKEY: UniqueKey<BookGenreRecord> = Internal.createUniqueKey(BookGenre.BOOK_GENRE, DSL.name("book_genre_pkey"), arrayOf(BookGenre.BOOK_GENRE.BOOK_ID, BookGenre.BOOK_GENRE.GENRE_ID), true)
val GENRE_NAME_KEY: UniqueKey<GenreRecord> = Internal.createUniqueKey(Genre.GENRE, DSL.name("genre_name_key"), arrayOf(Genre.GENRE.NAME), true)
val GENRE_PKEY: UniqueKey<GenreRecord> = Internal.createUniqueKey(Genre.GENRE, DSL.name("genre_pkey"), arrayOf(Genre.GENRE.ID), true)

// -------------------------------------------------------------------------
// FOREIGN KEY definitions
// -------------------------------------------------------------------------

val BOOK_AUTHOR__BOOK_AUTHOR_AUTHOR_ID_FKEY: ForeignKey<BookAuthorRecord, AuthorRecord> = Internal.createForeignKey(BookAuthor.BOOK_AUTHOR, DSL.name("book_author_author_id_fkey"), arrayOf(BookAuthor.BOOK_AUTHOR.AUTHOR_ID), generated.jooq.keys.AUTHOR_PKEY, arrayOf(Author.AUTHOR.ID), true)
val BOOK_AUTHOR__BOOK_AUTHOR_BOOK_ID_FKEY: ForeignKey<BookAuthorRecord, BookRecord> = Internal.createForeignKey(BookAuthor.BOOK_AUTHOR, DSL.name("book_author_book_id_fkey"), arrayOf(BookAuthor.BOOK_AUTHOR.BOOK_ID), generated.jooq.keys.BOOK_PKEY, arrayOf(Book.BOOK.ID), true)
val BOOK_GENRE__BOOK_GENRE_BOOK_ID_FKEY: ForeignKey<BookGenreRecord, BookRecord> = Internal.createForeignKey(BookGenre.BOOK_GENRE, DSL.name("book_genre_book_id_fkey"), arrayOf(BookGenre.BOOK_GENRE.BOOK_ID), generated.jooq.keys.BOOK_PKEY, arrayOf(Book.BOOK.ID), true)
val BOOK_GENRE__BOOK_GENRE_GENRE_ID_FKEY: ForeignKey<BookGenreRecord, GenreRecord> = Internal.createForeignKey(BookGenre.BOOK_GENRE, DSL.name("book_genre_genre_id_fkey"), arrayOf(BookGenre.BOOK_GENRE.GENRE_ID), generated.jooq.keys.GENRE_PKEY, arrayOf(Genre.GENRE.ID), true)
