/*
 * This file is generated by jOOQ.
 */
package generated.jooq.tables.daos


import generated.jooq.AbstractSpringDAOImpl
import generated.jooq.tables.Genre
import generated.jooq.tables.records.GenreRecord

import javax.annotation.processing.Generated

import kotlin.collections.List

import org.jooq.Configuration
import org.springframework.stereotype.Repository


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
@Repository
open class GenreDao(configuration: Configuration?) : AbstractSpringDAOImpl<GenreRecord, generated.jooq.tables.pojos.Genre, Long>(Genre.GENRE, generated.jooq.tables.pojos.Genre::class.java, configuration) {

    /**
     * Create a new GenreDao without any configuration
     */
    constructor(): this(null)

    override fun getId(o: generated.jooq.tables.pojos.Genre): Long? = o.id

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfId(lowerInclusive: Long?, upperInclusive: Long?): List<generated.jooq.tables.pojos.Genre> = fetchRange(Genre.GENRE.ID, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    fun fetchById(vararg values: Long): List<generated.jooq.tables.pojos.Genre> = fetch(Genre.GENRE.ID, *values.toTypedArray())

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    fun fetchOneById(value: Long): generated.jooq.tables.pojos.Genre? = fetchOne(Genre.GENRE.ID, value)

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND
     * upperInclusive</code>
     */
    fun fetchRangeOfName(lowerInclusive: String?, upperInclusive: String?): List<generated.jooq.tables.pojos.Genre> = fetchRange(Genre.GENRE.NAME, lowerInclusive, upperInclusive)

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    fun fetchByName(vararg values: String): List<generated.jooq.tables.pojos.Genre> = fetch(Genre.GENRE.NAME, *values)

    /**
     * Fetch a unique record that has <code>name = value</code>
     */
    fun fetchOneByName(value: String): generated.jooq.tables.pojos.Genre? = fetchOne(Genre.GENRE.NAME, value)
}
