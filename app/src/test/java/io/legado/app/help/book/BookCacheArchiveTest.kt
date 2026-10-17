package io.legado.app.help.book

import android.app.Application
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.domain.gateway.DownloadCacheSettingsGateway
import io.legado.app.domain.gateway.OtherSettingsGateway
import io.legado.app.domain.model.settings.DownloadCacheSettings
import io.legado.app.domain.model.settings.OtherSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
 * 缓存 zip 导出的结构契约：压缩包内一章一个目录、图片平铺在该目录下，
 * 没缓存的章节只留一个用自己的章节名命名的空目录。
 *
 * 这正是漫画导入识别章节的规则（`LocalMangaLoader.imageGroups`：按图片路径的父目录分组、
 * 只认图片扩展名），所以同一套结构在本地导入里可以直接当漫画添加。真正的加载链路走
 * libarchive JNI，JVM 单测里加载不了，这里按同一套规则校验压缩包结构。
 *
 * 只跑 sdk 26：再高的 SDK 需要 Robolectric 原生运行时，部分环境（如 Linux aarch64）不可用，
 * 而这部分逻辑与 SDK 无关。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [26])
class BookCacheArchiveTest {

    private val book = Book(bookUrl = "book-zip-contract", name = "Zip contract")

    /** 与 `LocalMangaLoader.IMAGE_EXTENSIONS` 保持一致。 */
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "bmp")

    @Before
    fun setUp() {
        RuntimeEnvironment.getApplication().injectAsAppCtx()
        startKoin {
            modules(module {
                single<DownloadCacheSettingsGateway> {
                    object : DownloadCacheSettingsGateway {
                        override val currentSettings = DownloadCacheSettings()
                        override val settings = flowOf(currentSettings)
                        override suspend fun update(transform: (DownloadCacheSettings) -> DownloadCacheSettings) = Unit
                    }
                }
                single<OtherSettingsGateway> {
                    object : OtherSettingsGateway {
                        override val currentSettings = OtherSettings()
                        override val settings = flowOf(currentSettings)
                        override suspend fun update(transform: (OtherSettings) -> OtherSettings) = Unit
                    }
                }
            })
        }
    }

    @After
    fun tearDown() = stopKoin()

    /** 条目只按扩展名筛选，字节内容不参与判定，所以不必造真图。 */
    private fun imageBytes(seed: Int): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), seed.toByte())

    /** 造一章图标缓存：正文记下图片顺序，图片按章内序号落盘。 */
    private fun cacheChapter(chapter: BookChapter, imageCount: Int) {
        val urls = (1..imageCount).map { "https://invalid.example/${chapter.index}-$it.jpg" }
        val content = urls.joinToString("") { "<img src=\"$it\"/>" }
        BookHelp.getChapterContentFile(book, chapter).apply { parentFile?.mkdirs() }
            .writeText(content)
        urls.forEachIndexed { index, src ->
            BookHelp.getImage(book, chapter, src).apply { parentFile?.mkdirs() }
                .writeBytes(imageBytes(index + 1))
        }
    }

    private fun chapters(): List<BookChapter> = listOf(
        BookChapter(
            bookUrl = book.bookUrl,
            url = "https://invalid.example/c1",
            title = "第1话 你好啊",
            index = 0,
        ),
        BookChapter(
            bookUrl = book.bookUrl,
            url = "https://invalid.example/c2",
            title = "第2话",
            index = 1,
        ),
        BookChapter(
            bookUrl = book.bookUrl,
            url = "https://invalid.example/c3",
            title = "第3话 未缓存",
            index = 2,
        ),
    )

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

    /** 漫画导入的分章规则：只认图片扩展名，按父目录分组。 */
    private fun Map<String, ByteArray>.comicChapters(): Map<String, List<String>> =
        keys.filter { it.substringAfterLast('.', "").lowercase() in imageExtensions }
            .groupBy { it.substringBeforeLast('/', "") }

    @Test
    fun `zip keeps one folder per chapter and skips store bookkeeping files`() = runBlocking {
        val (first, second) = chapters()
        try {
            cacheChapter(first, imageCount = 2)
            cacheChapter(second, imageCount = 1)
            val firstImage = BookHelp.getImage(book, first, "https://invalid.example/0-1.jpg")
            // 缓存的记账文件属于本地状态，不该出现在导出的压缩包里
            File(File(firstImage.parentFile, ".online"), firstImage.name).apply {
                parentFile?.mkdirs()
                writeText("online marker")
            }
            File(File(firstImage.parentFile, ".downloads"), firstImage.name).apply {
                parentFile?.mkdirs()
                writeText("explicit download marker")
            }
            File(firstImage.parentFile, "MAO-999.jpg.tmp").writeText("half written")

            val output = ByteArrayOutputStream()
            val cached = BookCacheArchive.write(book, listOf(first, second), output)

            assertEquals(2, cached)
            val entries = output.toByteArray().zipEntries()
            assertEquals(
                listOf(
                    "MAO-第1话 你好啊/",
                    "MAO-第1话 你好啊/MAO-001.jpg",
                    "MAO-第1话 你好啊/MAO-002.jpg",
                    "MAO-第1话 你好啊/MAO-第1话 你好啊.nb",
                    "MAO-第2话/",
                    "MAO-第2话/MAO-001.jpg",
                    "MAO-第2话/MAO-第2话.nb",
                ),
                entries.keys.toList(),
            )
            // 图片是原样拷进去的
            assertArrayEquals(imageBytes(1), entries["MAO-第1话 你好啊/MAO-001.jpg"])
        } finally {
            BookHelp.clearCache(book)
        }
    }

    @Test
    fun `uncached chapter keeps only its own folder name`() = runBlocking {
        val (first, second) = chapters()
        try {
            cacheChapter(first, imageCount = 1)

            val output = ByteArrayOutputStream()
            val cached = BookCacheArchive.write(book, listOf(first, second), output)

            assertEquals(1, cached)
            val names = output.toByteArray().zipEntries().keys
            assertEquals(
                listOf(
                    "MAO-第1话 你好啊/",
                    "MAO-第1话 你好啊/MAO-001.jpg",
                    "MAO-第1话 你好啊/MAO-第1话 你好啊.nb",
                    "MAO-第2话/",
                ),
                names.toList(),
            )
        } finally {
            BookHelp.clearCache(book)
        }
    }

    @Test
    fun `zip is laid out the way the comic importer groups chapters`() = runBlocking {
        val (first, second, uncached) = chapters()
        try {
            cacheChapter(first, imageCount = 2)
            cacheChapter(second, imageCount = 1)

            val output = ByteArrayOutputStream()
            assertEquals(2, BookCacheArchive.write(book, listOf(first, second, uncached), output))

            val groups = output.toByteArray().zipEntries().comicChapters()
            assertEquals(
                listOf("MAO-第1话 你好啊", "MAO-第2话"),
                groups.keys.toList(),
            )
            assertEquals(
                listOf(
                    "MAO-第1话 你好啊/MAO-001.jpg",
                    "MAO-第1话 你好啊/MAO-002.jpg",
                ),
                groups.getValue("MAO-第1话 你好啊"),
            )
            assertEquals(
                listOf("MAO-第2话/MAO-001.jpg"),
                groups.getValue("MAO-第2话"),
            )
        } finally {
            BookHelp.clearCache(book)
        }
    }
}
