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

    suspend fun existsByPathEquals(path: String): Boolean

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$group: { _id: { \$toUpper: { \$substrCP: ['\$authors.fullName', 0, 1] } }, count: { \$sum: 1 } } }",
            "{ \$sort: { _id: 1 } }"
        ]
    )
    fun findAuthorFirstLetters(): Flow<AuthorLetterResult>


    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$match: { 'authors.fullName': { \$regex: '^?0', \$options: 'i' } } }",
            "{ \$group: { _id: { \$toUpper: { \$substrCP: ['\$authors.fullName', 0, ?1] } }, count: { \$sum: 1 } } }",
            "{ \$sort: { _id: 1 } }"
        ]
    )
    fun findAuthorPrefixes(startingLetter: String, prefixLength: Int): Flow<AuthorLetterResult>

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$match: { 'authors.fullName': { \$regex: '^?0$', \$options: 'i' } } }",
            "{ \$count: 'count' }"
        ]
    )
    fun countExactLastNames(prefix: String): Flow<CountResult>

    @Aggregation(
        pipeline = [
            "{ \$unwind: '\$authors' }",
            "{ \$match: { 'authors.fullName': { \$regex: '^?0', \$options: 'i' } } }",
            "{ \$group: { _id: { fullName: '\$authors.fullName', lastName: '\$authors.lastName', firstName: '\$authors.firstName' } } }",
            "{ \$sort: { '_id.fullName': 1 } }"
        ]
    )
    fun findAuthorsByPrefix(prefix: String): Flow<AuthorResult>

    @Query("{ 'authors': { \$elemMatch: { 'fullName': ?0 } } }")
    fun findBooksByAuthorFullName(fullName: String, pageable: Pageable): Flow<Book>

    suspend fun countByAuthorsFullName(fullName: String): Long


    @Query("{ 'authors': { \$elemMatch: { 'fullName': ?0 } }, 'sequence': null }")
    fun findBooksByAuthorWithoutSeriesFullName(fullName: String, sort: Sort): Flow<Book>

    @Aggregation(
        pipeline = [
            "{ \$match: { 'authors': { \$elemMatch: { 'fullName': ?0 } }, 'sequence': { \$ne: null } } }",
            "{ \$group: { _id: '\$sequence' } }",
            "{ \$sort: { _id: 1 } }"
        ]
    )
    fun findSeriesByAuthorFullName(fullName: String): Flow<SeriesResult>

    @Query("{ 'sequence': ?0 }")
    fun findBooksBySeries(series: String, sort: Sort): Flow<Book>

    fun findAllBy():Flow<BookIndexItem>
}
