package io.legado.app.help.book

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 本地漫画分章规则：按图片父目录分章，每个目录哪怕只有一张图也算独立章节；
 * 直接放在根目录的图片才归入书名。导出的 CBZ 常出现「一目录一页」，这条规则决定它再导入后是否串章。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [26])
class LocalMangaChaptersGroupTest {

    @Test
    fun `one image per directory stays its own chapter`() {
        val grouped = LocalMangaChapters.group(
            listOf("第1话/001.jpg", "第2话/001.jpg", "第3话/001.png"),
            fallbackTitle = "book",
        )

        assertEquals(listOf("第1话", "第2话", "第3话"), grouped.keys.toList())
        assertEquals(listOf("第1话/001.jpg"), grouped.getValue("第1话"))
    }

    @Test
    fun `images at the root fall back to the book title`() {
        val grouped = LocalMangaChapters.group(
            listOf("002.jpg", "001.jpg"),
            fallbackTitle = "book",
        )

        assertEquals(mapOf("book" to listOf("001.jpg", "002.jpg")), grouped)
    }

    @Test
    fun `directory pages keep filename order`() {
        val grouped = LocalMangaChapters.group(
            listOf("第1话/002.jpg", "第1话/001.jpg", "第2话/001.webp"),
            fallbackTitle = "book",
        )

        assertEquals(listOf("第1话/001.jpg", "第1话/002.jpg"), grouped.getValue("第1话"))
    }
}
