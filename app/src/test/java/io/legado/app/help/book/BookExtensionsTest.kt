package io.legado.app.help.book

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.HighlightTagRule
import org.junit.Assert.assertEquals
import org.junit.Test

class BookExtensionsTest {

    @Test
    fun displayTags_keepsCustomAndSourceTagsSeparateAndDeduplicated() {
        val book = Book(
            kind = "奇幻,连载",
            customTag = "收藏,奇幻",
        )

        assertEquals(listOf("奇幻", "连载"), book.getSourceTagList())
        assertEquals(listOf("收藏", "奇幻"), book.getCustomTagList())
        assertEquals(listOf("收藏", "奇幻", "连载"), book.getDisplayTagList())
        assertEquals("奇幻,连载", book.kind)
        assertEquals("收藏,奇幻", book.customTag)
    }

    @Test
    fun chapterCacheFolderName_usesChapterTitle() {
        assertEquals(
            "MAO-序章 一二三四五六",
            BookChapter(bookUrl = "book", title = "序章 一二三四五六", index = 0).getCacheFolderName(),
        )
        assertEquals(
            "MAO-第12话 你好啊",
            BookChapter(bookUrl = "book", title = "第12话 你好啊", index = 11).getCacheFolderName(),
        )
        // 没有标题时补「第 N 话」。
        assertEquals(
            "MAO-第12话",
            BookChapter(bookUrl = "book", title = "", index = 11).getCacheFolderName(),
        )
        // 文件名非法字符换空格，连续空白压成一个。
        assertEquals(
            "MAO-休刊通知：下期休刊 恢复时间 另行通知",
            BookChapter(
                bookUrl = "book",
                title = "休刊通知：下期休刊/恢复时间\n另行通知",
                index = 20,
            ).getCacheFolderName(),
        )
    }

    @Test
    fun chapterCacheFileName_followsTheChapterFolderName() {
        val chapter = BookChapter(bookUrl = "book", title = "第1话 你好啊", index = 0)
        assertEquals("MAO-第1话 你好啊.nb", chapter.getFileName())
        assertEquals("MAO-第1话 你好啊.nr", chapter.getFileName("nr"))
        assertEquals(
            "MAO-第12话.nb",
            BookChapter(bookUrl = "book", title = "", index = 11).getFileName(),
        )
    }

    @Test
    fun parseHighlightedTags_ordersMatchesByRuleOrder() {
        val rules = listOf(
            HighlightTagRule(
                id = 2,
                title = "次要",
                pattern = "动作",
                order = 1,
            ),
            HighlightTagRule(
                id = 1,
                title = "优先",
                pattern = "奇幻",
                order = 0,
            ),
        )

        val (highlighted, regular) = parseHighlightedTags(
            kindLabels = listOf("动作", "普通", "奇幻"),
            rules = rules,
        )

        assertEquals(listOf("优先", "次要"), highlighted.map { it.title })
        assertEquals(listOf(listOf("奇幻"), listOf("动作")), highlighted.map { it.matchedLabels })
        assertEquals(listOf("普通"), regular)
    }
}
