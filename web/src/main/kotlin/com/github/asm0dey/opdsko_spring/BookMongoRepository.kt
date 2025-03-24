package com.github.asm0dey.opdsko_spring

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface BookMongoRepository : CoroutineCrudRepository<Book, String>, CoroutineSortingRepository<Book, String> {
    fun findAllBy(pageable: Pageable): Flow<Book>
    fun findAllByIdIn(ids: List<String>): Flow<Book>

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$group: { _id: { \$toUpper: { \$substrCP: ['\$authors.lastName', 0, 1] } }, count: { \$sum: 1 } } }",
            "{ \$sort: { _id: 1 } }"
        ]
    )
    fun findAuthorFirstLetters(): Flow<AuthorLetterResult>

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$match: { 'authors.lastName': { \$regex: '^?0', \$options: 'i' } } }",
            "{ \$group: { _id: { \$toUpper: { \$substrCP: ['\$authors.lastName', 0, ?1] } }, count: { \$sum: 1 } } }",
            "{ \$sort: { _id: 1 } }"
        ]
    )
    fun findAuthorPrefixes(startingLetter: String, prefixLength: Int): Flow<AuthorLetterResult>

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$match: { 'authors.lastName': { \$regex: '^?0$', \$options: 'i' } } }",
            "{ \$count: 'count' }"
        ]
    )
    fun countExactLastNames(prefix: String): Flow<CountResult>

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$match: { 'authors.lastName': { \$regex: '^?0', \$options: 'i' } } }",
            "{ \$group: { _id: { lastName: '\$authors.lastName', firstName: '\$authors.firstName' } } }",
            "{ \$sort: { '_id.lastName': 1, '_id.firstName': 1 } }"
        ]
    )
    fun findAuthorsByPrefix(prefix: String): Flow<AuthorResult>

    @Query("{ 'authors': { \$elemMatch: { 'lastName': ?0, 'firstName': ?1 } } }")
    fun findBooksByAuthor(lastName: String, firstName: String, sort: Sort): Flow<Book>

    @Query("{ 'authors': { \$elemMatch: { 'lastName': ?0, 'firstName': ?1 } }, 'sequence': null }")
    fun findBooksByAuthorWithoutSeries(lastName: String, firstName: String, sort: Sort): Flow<Book>

    @Aggregation(
        pipeline = [
            "{ \$match: { 'authors': { \$elemMatch: { 'lastName': ?0, 'firstName': ?1 } }, 'sequence': { \$ne: null } } }",
            "{ \$group: { _id: '\$sequence' } }",
            "{ \$sort: { _id: 1 } }"
        ]
    )
    fun findSeriesByAuthor(lastName: String, firstName: String): Flow<SeriesResult>

    @Query("{ 'sequence': ?0 }")
    fun findBooksBySeries(series: String, sort: Sort): Flow<Book>

    fun findAllByPathIn(paths: List<String>): Flow<Book>
}
