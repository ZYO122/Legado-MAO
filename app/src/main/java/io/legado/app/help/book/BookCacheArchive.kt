package io.legado.app.help.book

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 把章节缓存打成 zip：压缩包内一章一个目录，目录名就是缓存目录名（`MAO-第1话 你好啊`），
 * 章内图片从 `<章节目录>/images` 提上来直接放在该目录下。
 *
 * 这个结构同时是漫画导入识别的结构（[io.legado.app.data.repository.manga.LocalMangaLoader]
 * 按图片路径的父目录分章），所以导出的压缩包可以在本地导入里直接当漫画添加。
 */
object BookCacheArchive {

    /** 章节缓存里的图片目录名，与 [BookHelp] 的落盘位置一致。 */
    private const val imageFolderName = "images"

    private val imageExtensions =
        setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "bmp")

    /** zip 条目名 → 源文件。 */
    data class Entry(val name: String, val file: File)

    /** 该章缓存要打包的条目；没有缓存时返回空。 */
    fun chapterEntries(book: Book, chapter: BookChapter): List<Entry> {
        val contentFile = BookHelp.getChapterContentFile(book, chapter)
        val chapterDir = contentFile.parentFile ?: return emptyList()
        val folder = chapter.getCacheFolderName()
        val entries = ArrayList<Entry>()
        // 新版正文与章节目录同名；改名之前的旧正文仍留在书目录下，一并带上。
        val content = if (contentFile.isFile) {
            contentFile
        } else {
            chapterDir.parentFile?.let { File(it, chapter.getFileName("nb")) }
        }
        if (content?.isFile == true) {
            entries += Entry("$folder/${content.name}", content)
        }
        File(chapterDir, imageFolderName).listFiles()?.forEach { file ->
            val suffix = file.name.substringAfterLast('.', "").lowercase()
            if (file.isFile && !file.name.endsWith(".tmp") && suffix in imageExtensions) {
                entries += Entry("$folder/${file.name}", file)
            }
        }
        return entries.sortedBy { it.name }
    }

    /**
     * 按章写入 zip，返回有缓存的章节数。
     *
     * 范围内每一章都会先写入自己的目录项：没缓存的章节只留一个空目录，
     * 这样压缩包里能看到全书（或所选区间）的章节名与序号，缺哪几章一目了然。
     * [onChapterProcessed] 每章处理完回调一次已处理章数，供调用方上报进度。
     */
    fun write(
        book: Book,
        chapters: List<BookChapter>,
        output: OutputStream,
        onChapterProcessed: (processedCount: Int) -> Unit = {},
    ): Int {
        var cached = 0
        ZipOutputStream(output.buffered()).use { zip ->
            chapters.forEachIndexed { index, chapter ->
                val folder = chapter.getCacheFolderName()
                zip.putNextEntry(ZipEntry("$folder/"))
                zip.closeEntry()
                val entries = chapterEntries(book, chapter)
                if (entries.isNotEmpty()) {
                    entries.forEach { entry ->
                        zip.putNextEntry(ZipEntry(entry.name))
                        entry.file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                    cached++
                }
                onChapterProcessed(index + 1)
            }
        }
        return cached
    }
}
