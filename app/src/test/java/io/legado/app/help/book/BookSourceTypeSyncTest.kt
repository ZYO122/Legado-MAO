package io.legado.app.help.book

import io.legado.app.constant.BookSourceType
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import org.junit.Assert.assertEquals
import org.junit.Test

class BookSourceTypeSyncTest {

    @Test
    fun `new book adopts image source type`() {
        val book = Book(type = BookType.text)

        book.applySourceType(BookSource(bookSourceType = BookSourceType.image))

        assertEquals(BookType.image, book.type)
    }

    @Test
    fun `source type sync keeps not shelf flag`() {
        val book = Book(type = BookType.text or BookType.notShelf)

        book.applySourceType(BookSource(bookSourceType = BookSourceType.image))

        assertEquals(BookType.image or BookType.notShelf, book.type)
    }

    @Test
    fun `text source replaces stale image type`() {
        val book = Book(type = BookType.image)

        book.applySourceType(BookSource(bookSourceType = BookSourceType.default))

        assertEquals(BookType.text, book.type)
    }
}
