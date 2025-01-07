/*
 * This file is generated by jOOQ.
 */
package generated.jooq.tables.records


import generated.jooq.tables.Author
import generated.jooq.tables.interfaces.IAuthor

import java.time.OffsetDateTime

import javax.annotation.processing.Generated

import org.jooq.Record1
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
open class AuthorRecord() : UpdatableRecordImpl<AuthorRecord>(Author.AUTHOR), IAuthor {

    open override var id: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    open override var fb2id: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open override var firstName: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open override var middleName: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open override var lastName: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    open override var nickname: String?
        set(value): Unit = set(5, value)
        get(): String? = get(5) as String?

    open override var added: OffsetDateTime?
        set(value): Unit = set(6, value)
        get(): OffsetDateTime? = get(6) as OffsetDateTime?

    open override var fullName: String?
        set(value): Unit = set(7, value)
        get(): String? = get(7) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Long?> = super.key() as Record1<Long?>

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    fun from(from: IAuthor) {
        this.id = from.id
        this.fb2id = from.fb2id
        this.firstName = from.firstName
        this.middleName = from.middleName
        this.lastName = from.lastName
        this.nickname = from.nickname
        this.added = from.added
        this.fullName = from.fullName
        resetChangedOnNotNull()
    }

    /**
     * Create a detached, initialised AuthorRecord
     */
    constructor(id: Long? = null, fb2id: String? = null, firstName: String? = null, middleName: String? = null, lastName: String? = null, nickname: String? = null, added: OffsetDateTime? = null, fullName: String? = null): this() {
        this.id = id
        this.fb2id = fb2id
        this.firstName = firstName
        this.middleName = middleName
        this.lastName = lastName
        this.nickname = nickname
        this.added = added
        this.fullName = fullName
        resetChangedOnNotNull()
    }

    /**
     * Create a detached, initialised AuthorRecord
     */
    constructor(value: generated.jooq.tables.pojos.Author?): this() {
        if (value != null) {
            this.id = value.id
            this.fb2id = value.fb2id
            this.firstName = value.firstName
            this.middleName = value.middleName
            this.lastName = value.lastName
            this.nickname = value.nickname
            this.added = value.added
            this.fullName = value.fullName
            resetChangedOnNotNull()
        }
    }
}
