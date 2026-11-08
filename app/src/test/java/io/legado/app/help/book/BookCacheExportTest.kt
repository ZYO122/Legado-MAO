package io.legado.app.help.book

import android.app.Application
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.domain.gateway.DownloadCacheSettingsGateway
import io.legado.app.domain.gateway.OtherSettingsGateway
import io.legado.app.domain.model.settings.DownloadCacheSettings
import io.legado.app.domain.model.settings.OtherSettings
import io.legado.app.utils.FileDoc
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 缓存结构导出的契约：一章一个目录，文本模式写同名 `.txt` 与本章图片，漫画模式只写图片，
 * 没缓存的章节只留一个带章节名的空目录。
 *
 * 只跑 sdk 26：更高的 SDK 需要 Robolectric 原生运行时，部分环境（如 Linux aarch64）不可用，
 * 而这里只涉及文件与 zip 结构，与 SDK 无关。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [26])
class BookCacheExportTest {

    private val book = Book(bookUrl = "book-cache-export", name = "Cache export")

    private val first = BookChapter(
        bookUrl = book.bookUrl,
        url = "https://invalid.example/c1",
        title = "第1话 你好啊",
        index = 0,
    )
    private val second = BookChapter(
        bookUrl = book.bookUrl,
        url = "https://invalid.example/c2",
        title = "第2话 未缓存",
        index = 1,
    )

    private lateinit var targetDir: File

    @Before
    fun setUp() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
        startKoin {
            modules(module {
                single<DownloadCacheSettingsGateway> {
                    object : DownloadCacheSettingsGateway {
                        override val currentSettings = DownloadCacheSettings()
                        override val settings = flowOf(currentSettings)
                        override suspend fun update(
                            transform: (DownloadCacheSettings) -> DownloadCacheSettings,
                        ) = Unit
                    }
                }
                single<OtherSettingsGateway> {
                    object : OtherSettingsGateway {
                        override val currentSettings = OtherSettings()
                        override val settings = flowOf(currentSettings)
                        override suspend fun update(
                            transform: (OtherSettings) -> OtherSettings,
                        ) = Unit
                    }
                }
            })
        }
        targetDir = File(
            RuntimeEnvironment.getApplication().cacheDir,
            "book-cache-export-${System.nanoTime()}",
        ).apply { mkdirs() }
    }

    @After
    fun tearDown() {
        BookHelp.clearCache(book)
        targetDir.deleteRecursively()
        stopKoin()
    }

    /** 条目只按扩展名筛选，字节内容不参与判定，所以不必造真图。 */
    private fun imageBytes(seed: Int): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), seed.toByte())

    private fun cacheChapter(chapter: BookChapter, imageCount: Int) {
        val urls = (1..imageCount).map { "https://invalid.example/${chapter.index}-$it.jpg" }
        BookHelp.getChapterContentFile(book, chapter).apply { parentFile?.mkdirs() }
            .writeText(urls.joinToString("") { "<img src=\"$it\"/>" })
        urls.forEachIndexed { index, src ->
            BookHelp.getImage(book, chapter, src).apply { parentFile?.mkdirs() }
                .writeBytes(imageBytes(index + 1))
        }
    }

    private fun target() = FileDoc.fromDir(targetDir.absolutePath)

    private fun ByteArray.zipEntries(): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    @Test
    fun `folder keeps one directory per chapter with its images only`() = runBlocking {
        cacheChapter(first, imageCount = 2)

        val written = BookCacheExport.writeFolder(
            target = target(),
            book = book,
            chapters = listOf(first, second),
        )

        assertEquals(1, written)
        val bookDir = File(targetDir, "Cache export")
        val firstDir = File(bookDir, "MAO-第1话 你好啊")
        assertEquals(listOf("MAO-001.jpg", "MAO-002.jpg"), firstDir.listFiles()?.map { it.name }?.sorted())
        // 没缓存的章节只留章节名
        val secondDir = File(bookDir, "MAO-第2话 未缓存")
        assertTrue(secondDir.isDirectory)
        assertEquals(0, secondDir.listFiles()?.size ?: 0)
    }

    @Test
    fun `manga folder keeps images only and no chapter text`() = runBlocking {
        cacheChapter(first, imageCount = 1)

        val written = BookCacheExport.writeFolder(
            target = target(),
            book = book,
            chapters = listOf(first, second),
        )

        assertEquals(1, written)
        val firstDir = File(File(targetDir, "Cache export"), "MAO-第1话 你好啊")
        assertEquals(listOf("MAO-001.jpg"), firstDir.listFiles()?.map { it.name }?.sorted())
        assertTrue(File(File(targetDir, "Cache export"), "MAO-第2话 未缓存").isDirectory)
    }

    @Test
    fun `cbz keeps the cache layout without chapter text`() = runBlocking {
        cacheChapter(first, imageCount = 2)

        val output = ByteArrayOutputStream()
        val written = BookCacheExport.writeCbz(book, listOf(first, second), output)

        assertEquals(1, written)
        assertEquals(
            listOf(
                "MAO-第1话 你好啊/",
                "MAO-第1话 你好啊/MAO-001.jpg",
                "MAO-第1话 你好啊/MAO-002.jpg",
                "MAO-第2话 未缓存/",
            ),
            output.toByteArray().zipEntries().keys.toList(),
        )
    }

    @Test
    fun `existing book folder gets a numbered suffix`() = runBlocking {
        cacheChapter(first, imageCount = 1)
        File(targetDir, "Cache export").mkdirs()

        BookCacheExport.writeFolder(target(), book, listOf(first))

        assertTrue(File(File(targetDir, "Cache export (2)"), "MAO-第1话 你好啊/MAO-001.jpg").isFile)
    }

    @Test
    fun `nothing cached leaves no folder behind`() = runBlocking {
        val written = BookCacheExport.writeFolder(
            target = target(),
            book = book,
            chapters = listOf(first, second),
        )

        assertEquals(0, written)
        assertFalse(File(targetDir, "Cache export").exists())
    }
}
