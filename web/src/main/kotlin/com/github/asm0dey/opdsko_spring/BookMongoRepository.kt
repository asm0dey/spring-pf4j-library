package com.github.asm0dey.opdsko_spring

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface BookMongoRepository : CoroutineCrudRepository<Book, String>, CoroutineSortingRepository<Book, String> {
    fun findAllBy(pageable: Pageable): Flow<Book>
    fun findAllByIdIn(ids: List<String>): Flow<Book>
}