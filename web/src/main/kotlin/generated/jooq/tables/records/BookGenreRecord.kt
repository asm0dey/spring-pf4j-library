/*
 * This file is generated by jOOQ.
 */
package generated.jooq.tables.records


import generated.jooq.tables.BookGenre
import generated.jooq.tables.interfaces.IBookGenre

import javax.annotation.processing.Generated

import org.jooq.Record2
import org.jooq.impl.UpdatableRecordImpl


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
open class BookGenreRecord() : UpdatableRecordImpl<BookGenreRecord>(BookGenre.BOOK_GENRE), IBookGenre {

    open override var bookId: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    open override var genreId: Long?
        set(value): Unit = set(1, value)
        get(): Long? = get(1) as Long?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record2<Long?, Long?> = super.key() as Record2<Long?, Long?>

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    fun from(from: IBookGenre) {
        this.bookId = from.bookId
        this.genreId = from.genreId
        resetChangedOnNotNull()
    }

    /**
     * Create a detached, initialised BookGenreRecord
     */
    constructor(bookId: Long? = null, genreId: Long? = null): this() {
        this.bookId = bookId
        this.genreId = genreId
        resetChangedOnNotNull()
    }

    /**
     * Create a detached, initialised BookGenreRecord
     */
    constructor(value: generated.jooq.tables.pojos.BookGenre?): this() {
        if (value != null) {
            this.bookId = value.bookId
            this.genreId = value.genreId
            resetChangedOnNotNull()
        }
    }
}
