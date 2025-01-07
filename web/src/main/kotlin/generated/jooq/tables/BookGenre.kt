/*
 * This file is generated by jOOQ.
 */
package generated.jooq.tables


import generated.jooq.Public
import generated.jooq.keys.BOOK_GENRE_PKEY
import generated.jooq.keys.BOOK_GENRE__BOOK_GENRE_BOOK_ID_FKEY
import generated.jooq.keys.BOOK_GENRE__BOOK_GENRE_GENRE_ID_FKEY
import generated.jooq.tables.Book.BookPath
import generated.jooq.tables.Genre.GenrePath
import generated.jooq.tables.records.BookGenreRecord

import javax.annotation.processing.Generated

import kotlin.collections.Collection
import kotlin.collections.List

import org.jooq.Condition
import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.InverseForeignKey
import org.jooq.Name
import org.jooq.Path
import org.jooq.PlainSQL
import org.jooq.QueryPart
import org.jooq.Record
import org.jooq.SQL
import org.jooq.Schema
import org.jooq.Select
import org.jooq.Stringly
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


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
open class BookGenre(
    alias: Name,
    path: Table<out Record>?,
    childPath: ForeignKey<out Record, BookGenreRecord>?,
    parentPath: InverseForeignKey<out Record, BookGenreRecord>?,
    aliased: Table<BookGenreRecord>?,
    parameters: Array<Field<*>?>?,
    where: Condition?
): TableImpl<BookGenreRecord>(
    alias,
    Public.PUBLIC,
    path,
    childPath,
    parentPath,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table(),
    where,
) {
    companion object {

        /**
         * The reference instance of <code>public.book_genre</code>
         */
        val BOOK_GENRE: BookGenre = BookGenre()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<BookGenreRecord> = BookGenreRecord::class.java

    /**
     * The column <code>public.book_genre.book_id</code>.
     */
    val BOOK_ID: TableField<BookGenreRecord, Long?> = createField(DSL.name("book_id"), SQLDataType.BIGINT.nullable(false), this, "")

    /**
     * The column <code>public.book_genre.genre_id</code>.
     */
    val GENRE_ID: TableField<BookGenreRecord, Long?> = createField(DSL.name("genre_id"), SQLDataType.BIGINT.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<BookGenreRecord>?): this(alias, null, null, null, aliased, null, null)
    private constructor(alias: Name, aliased: Table<BookGenreRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, null, aliased, parameters, null)
    private constructor(alias: Name, aliased: Table<BookGenreRecord>?, where: Condition?): this(alias, null, null, null, aliased, null, where)

    /**
     * Create an aliased <code>public.book_genre</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>public.book_genre</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>public.book_genre</code> table reference
     */
    constructor(): this(DSL.name("book_genre"), null)

    constructor(path: Table<out Record>, childPath: ForeignKey<out Record, BookGenreRecord>?, parentPath: InverseForeignKey<out Record, BookGenreRecord>?): this(Internal.createPathAlias(path, childPath, parentPath), path, childPath, parentPath, BOOK_GENRE, null, null)

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    @Generated(
        value = [
            "https://www.jooq.org",
            "jOOQ version:3.19.16",
            "schema version:0"
        ],
        comments = "This class is generated by jOOQ"
    )
    open class BookGenrePath : BookGenre, Path<BookGenreRecord> {
        constructor(path: Table<out Record>, childPath: ForeignKey<out Record, BookGenreRecord>?, parentPath: InverseForeignKey<out Record, BookGenreRecord>?): super(path, childPath, parentPath)
        private constructor(alias: Name, aliased: Table<BookGenreRecord>): super(alias, aliased)
        override fun `as`(alias: String): BookGenrePath = BookGenrePath(DSL.name(alias), this)
        override fun `as`(alias: Name): BookGenrePath = BookGenrePath(alias, this)
        override fun `as`(alias: Table<*>): BookGenrePath = BookGenrePath(alias.qualifiedName, this)
    }
    override fun getSchema(): Schema? = if (aliased()) null else Public.PUBLIC
    override fun getPrimaryKey(): UniqueKey<BookGenreRecord> = BOOK_GENRE_PKEY
    override fun getReferences(): List<ForeignKey<BookGenreRecord, *>> = listOf(BOOK_GENRE__BOOK_GENRE_BOOK_ID_FKEY, BOOK_GENRE__BOOK_GENRE_GENRE_ID_FKEY)

    private lateinit var _book: BookPath

    /**
     * Get the implicit join path to the <code>public.book</code> table.
     */
    fun book(): BookPath {
        if (!this::_book.isInitialized)
            _book = BookPath(this, BOOK_GENRE__BOOK_GENRE_BOOK_ID_FKEY, null)

        return _book;
    }

    val book: BookPath
        get(): BookPath = book()

    private lateinit var _genre: GenrePath

    /**
     * Get the implicit join path to the <code>public.genre</code> table.
     */
    fun genre(): GenrePath {
        if (!this::_genre.isInitialized)
            _genre = GenrePath(this, BOOK_GENRE__BOOK_GENRE_GENRE_ID_FKEY, null)

        return _genre;
    }

    val genre: GenrePath
        get(): GenrePath = genre()
    override fun `as`(alias: String): BookGenre = BookGenre(DSL.name(alias), this)
    override fun `as`(alias: Name): BookGenre = BookGenre(alias, this)
    override fun `as`(alias: Table<*>): BookGenre = BookGenre(alias.qualifiedName, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): BookGenre = BookGenre(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): BookGenre = BookGenre(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): BookGenre = BookGenre(name.qualifiedName, null)

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Condition?): BookGenre = BookGenre(qualifiedName, if (aliased()) this else null, condition)

    /**
     * Create an inline derived table from this table
     */
    override fun where(conditions: Collection<Condition>): BookGenre = where(DSL.and(conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(vararg conditions: Condition?): BookGenre = where(DSL.and(*conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Field<Boolean?>?): BookGenre = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(condition: SQL): BookGenre = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String): BookGenre = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg binds: Any?): BookGenre = where(DSL.condition(condition, *binds))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg parts: QueryPart): BookGenre = where(DSL.condition(condition, *parts))

    /**
     * Create an inline derived table from this table
     */
    override fun whereExists(select: Select<*>): BookGenre = where(DSL.exists(select))

    /**
     * Create an inline derived table from this table
     */
    override fun whereNotExists(select: Select<*>): BookGenre = where(DSL.notExists(select))
}
