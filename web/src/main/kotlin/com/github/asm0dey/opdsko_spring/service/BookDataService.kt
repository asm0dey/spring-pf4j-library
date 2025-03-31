package com.github.asm0dey.opdsko_spring.service

import com.github.asm0dey.opdsko_spring.Book
import com.github.asm0dey.opdsko_spring.repo.BookMongoRepository
import com.github.asm0dey.opdsko_spring.repo.BookRepo
import com.github.asm0dey.opdsko_spring.repo.PagedBooks
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * Service responsible for database operations related to books.
 */
@Service
class BookDataService(
    private val bookMongoRepository: BookMongoRepository,
    private val bookRepo: BookRepo
) {
    /**
     * Gets a book by its ID.
     *
     * @param id The ID of the book
     * @return The book, or null if not found
     */
    suspend fun getBookById(id: String): Book? {
        return bookMongoRepository.findById(id)
    }

    /**
     * Searches for books by name.
     *
     * @param searchTerm The search term
     * @param page The page number
     * @return A list of books matching the search term
     */
    suspend fun searchBookByName(searchTerm: String, page: Int): List<Book> =
        bookRepo.searchBookByName(searchTerm, page)

    /**
     * Gets the newest books.
     *
     * @param page The page number
     * @return A paged list of books
     */
    suspend fun newBooks(page: Int): PagedBooks = bookRepo.newBooks(page)

    /**
     * Saves a book to the database.
     *
     * @param book The book to save
     * @return The saved book
     */
    suspend fun saveBook(book: Book) = bookMongoRepository.save(book)

    /**
     * Finds books by series.
     *
     * @param series The series name
     * @param sort The sort order
     * @return A flow of books in the series
     */
    suspend fun findBooksBySeries(series: String, sort: Sort) = bookMongoRepository.findBooksBySeries(series, sort)

    /**
     * Finds books by author without series.
     *
     * @param fullName The author's full name
     * @param sort The sort order
     * @return A flow of books by the author without a series
     */
    suspend fun findBooksByAuthorWithoutSeriesFullName(fullName: String, sort: Sort) =
        bookMongoRepository.findBooksByAuthorWithoutSeriesFullName(fullName, sort)

    /**
     * Finds books by author.
     *
     * @param fullName The author's full name
     * @param page The page number
     * @return A paged list of books by the author
     */
    suspend fun findBooksByAuthorFullName(fullName: String, page: Int): PagedBooks {
        val pageable = PageRequest.of(page, 24, Sort.by(Sort.Direction.ASC, "name"))
        val books = bookMongoRepository.findBooksByAuthorFullName(fullName, pageable).toList()
        val total = bookMongoRepository.countByAuthorsFullName(fullName)
        return PagedBooks(books, total)
    }
}