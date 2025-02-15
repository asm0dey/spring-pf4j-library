/*
 * This file is generated by jOOQ.
 */
package generated.jooq.tables.records


import generated.jooq.tables.BookAuthor
import generated.jooq.tables.interfaces.IBookAuthor

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
open class BookAuthorRecord() : UpdatableRecordImpl<BookAuthorRecord>(BookAuthor.BOOK_AUTHOR), IBookAuthor {

    open override var bookId: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    open override var authorId: Long?
        set(value): Unit = set(1, value)
        get(): Long? = get(1) as Long?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record2<Long?, Long?> = super.key() as Record2<Long?, Long?>

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    fun from(from: IBookAuthor) {
        this.bookId = from.bookId
        this.authorId = from.authorId
        resetChangedOnNotNull()
    }

    /**
     * Create a detached, initialised BookAuthorRecord
     */
    constructor(bookId: Long? = null, authorId: Long? = null): this() {
        this.bookId = bookId
        this.authorId = authorId
        resetChangedOnNotNull()
    }

    /**
     * Create a detached, initialised BookAuthorRecord
     */
    constructor(value: generated.jooq.tables.pojos.BookAuthor?): this() {
        if (value != null) {
            this.bookId = value.bookId
            this.authorId = value.authorId
            resetChangedOnNotNull()
        }
    }
}
