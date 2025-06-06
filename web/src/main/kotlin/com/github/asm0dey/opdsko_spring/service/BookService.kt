package com.github.asm0dey.opdsko_spring.service

import com.github.asm0dey.opdsko_spring.Book
import com.github.asm0dey.opdsko_spring.repo.PagedBooks
import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.DependsOn
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import com.github.asm0dey.opdsko.common.Book as CommonBook

/**
 * Main service for book operations.
 * This service delegates to more specialized services for specific operations.
 */
@Service
@DependsOn("springPluginManager")
class BookService(
    private val bookDataService: BookDataService,
    private val bookFileService: BookFileService,
    private val bookConversionService: BookConversionService,
    private val authorService: AuthorService
) {
    /**
     * Gets the cover content types for a list of books.
     *
     * @param books The list of books
     * @return A map of book IDs to cover content types
     */
    fun imageTypes(books: List<Book>) = bookFileService.imageTypes(books)

    /**
     * Obtains a book from a path.
     *
     * @param path The path to the book
     * @return The book, or null if not found
     */
    fun obtainBook(path: String): CommonBook? = bookFileService.obtainBook(path)

    /**
     * Obtains books from a path.
     *
     * @param absolutePath The absolute path to the books
     * @return A flow of books and their sizes
     */
    fun obtainBooks(absolutePath: String): Flow<Pair<CommonBook, Long>> =
        bookFileService.obtainBooks(absolutePath)

    /**
     * Gets short descriptions for a list of books.
     *
     * @param bookWithInfos The list of books
     * @return A map of book IDs to short descriptions
     */
    suspend fun shortDescriptions(bookWithInfos: List<Book>) =
        bookFileService.shortDescriptions(bookWithInfos)

    /**
     * Gets a book by its ID.
     *
     * @param id The ID of the book
     * @return The book, or null if not found
     */
    suspend fun getBookById(id: String): Book? = bookDataService.getBookById(id)

    /**
     * Checks if a book exists.
     *
     * @param path The path to the book
     * @return True if the book exists, false otherwise
     */
    fun bookExists(path: String): Boolean = bookFileService.bookExists(path)

    /**
     * Searches for books by name.
     *
     * @param searchTerm The search term
     * @param page The page number
     * @return A list of books matching the search term
     */
    suspend fun searchBookByName(searchTerm: String, page: Int): List<Book> =
        bookDataService.searchBookByName(searchTerm, page)

    /**
     * Gets the newest books.
     *
     * @param page The page number
     * @return A paged list of books
     */
    suspend fun newBooks(page: Int): PagedBooks = bookDataService.newBooks(page)

    /**
     * Finds the first letters of author names.
     *
     * @return A flow of first letters
     */
    suspend fun findAuthorFirstLetters() = authorService.findAuthorFirstLetters()

    /**
     * Counts the exact last names with a given prefix.
     *
     * @param prefix The prefix
     * @return A flow of counts
     */
    suspend fun countExactLastNames(prefix: String) = authorService.countExactLastNames(prefix)

    /**
     * Finds author prefixes with a given prefix and length.
     *
     * @param prefix The prefix
     * @param prefixLength The length of the prefix
     * @return A flow of prefixes
     */
    suspend fun findAuthorPrefixes(prefix: String, prefixLength: Int) =
        authorService.findAuthorPrefixes(prefix, prefixLength)

    /**
     * Finds authors by prefix.
     *
     * @param prefix The prefix
     * @return A flow of authors
     */
    suspend fun findAuthorsByPrefix(prefix: String) = authorService.findAuthorsByPrefix(prefix)

    /**
     * Finds series by author full name.
     *
     * @param fullName The author's full name
     * @return A flow of series
     */
    suspend fun findSeriesByAuthorFullName(fullName: String) =
        authorService.findSeriesByAuthorFullName(fullName)

    /**
     * Finds books by series.
     *
     * @param series The series name
     * @param sort The sort order
     * @return A flow of books in the series
     */
    suspend fun findBooksBySeries(series: String, sort: Sort) =
        bookDataService.findBooksBySeries(series, sort)

    /**
     * Finds books by series and author.
     *
     * @param series The series name
     * @param fullName The author's full name
     * @param sort The sort order
     * @return A flow of books in the series by the author
     */
    suspend fun findBooksBySeriesAndAuthorFullName(series: String, fullName: String, sort: Sort) =
        bookDataService.findBooksBySeriesAndAuthorFullName(series, fullName, sort)

    /**
     * Finds books by author without series.
     *
     * @param fullName The author's full name
     * @param sort The sort order
     * @return A flow of books by the author without a series
     */
    suspend fun findBooksByAuthorWithoutSeriesFullName(fullName: String, sort: Sort) =
        bookDataService.findBooksByAuthorWithoutSeriesFullName(fullName, sort)

    /**
     * Finds books by author.
     *
     * @param fullName The author's full name
     * @param page The page number
     * @return A paged list of books by the author
     */
    suspend fun findBooksByAuthorFullName(fullName: String, page: Int): PagedBooks =
        bookDataService.findBooksByAuthorFullName(fullName, page)

    /**
     * Gets a book cover preview image.
     *
     * @param bookId The ID of the book
     * @return OperationResultWithData containing the cover data if successful
     */
    suspend fun getBookCover(bookId: String): OperationResultWithData<BookCoverData> =
        bookFileService.getBookCover(bookId)

    /**
     * Gets a full-size book cover image.
     *
     * @param bookId The ID of the book
     * @return OperationResultWithData containing the full-size cover data if successful
     */
    suspend fun getFullBookCover(bookId: String): OperationResultWithData<BookCoverData> =
        bookFileService.getFullBookCover(bookId)

    /**
     * Prepares a book for download, optionally converting it to a different format.
     *
     * @param bookId The ID of the book to download
     * @param targetFormat Optional format to convert the book to
     * @return OperationResultWithData containing the download data if successful
     */
    suspend fun downloadBook(bookId: String, targetFormat: String? = null): OperationResultWithData<BookDownloadData> =
        bookConversionService.downloadBook(bookId, targetFormat)
}
