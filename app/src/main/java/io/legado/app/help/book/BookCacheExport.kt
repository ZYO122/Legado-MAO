package io.legado.app.help.book

import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.FileDoc
import io.legado.app.utils.createFileIfNotExist
import io.legado.app.utils.createFolderIfNotExist
import io.legado.app.utils.delete
import io.legado.app.utils.list
import io.legado.app.utils.openOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 按本地缓存结构导出漫画章节。
 *
 * 文件夹：`<导出目录>/<书名>/MAO-第1话 你好啊/`，只放本章缓存的原图；
 * CBZ：条目为 `MAO-第1话 你好啊/MAO-001.jpg`。没缓存的章节只留一个带章节名的空目录
 * （CBZ 里是目录项），再导入时结构与导出时一致。
 */
object BookCacheExport {

    /** 章节图片目录名，与 [BookHelp] 的落盘位置一致。 */
    private const val imageFolderName = "images"

    private val imageExtensions =
        setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "bmp")

    /** 书名目录与章节目录共用的标题长度上限，与章节缓存命名一致。 */
    private const val maxNameLength = 60

    /**
     * 写入文件夹结构，返回有内容的章节数。
     *
     * 一章都没有内容时删掉本次新建的书名目录，调用方据此报错，不留下空目录。
     */
    fun writeFolder(
        target: FileDoc,
        book: Book,
        chapters: List<BookChapter>,
        onChapterProcessed: (processedCount: Int) -> Unit = {},
    ): Int {
        val bookDir = target.createFolderIfNotExist(uniqueFolderName(target, bookFolderName(book)))
        var written = 0
        chapters.forEachIndexed { index, chapter ->
            val folderName = chapter.getCacheFolderName()
            val chapterDir = bookDir.createFolderIfNotExist(folderName)
            var hasContent = false
            chapterImageFiles(book, chapter).forEach { image ->
                if (copyImageInto(chapterDir, image)) {
                    hasContent = true
                }
            }
            if (hasContent) {
                written++
            }
            onChapterProcessed(index + 1)
        }
        if (written == 0) {
            bookDir.delete()
        }
        return written
    }

    /**
     * 写入 CBZ，返回有图片的章节数；每章先写自己的目录项，没图片的章节只留目录。
     */
    fun writeCbz(
        book: Book,
        chapters: List<BookChapter>,
        output: OutputStream,
        onChapterProcessed: (processedCount: Int) -> Unit = {},
    ): Int {
        var written = 0
        ZipOutputStream(output.buffered()).use { zip ->
            chapters.forEachIndexed { index, chapter ->
                val folderName = chapter.getCacheFolderName()
                zip.putNextEntry(ZipEntry("$folderName/"))
                zip.closeEntry()
                val images = chapterImageFiles(book, chapter)
                var copied = 0
                images.forEach { image ->
                    BookHelp.pinImageFile(image).use {
                        // 导出可能持续很久，复制期间持有租约，避免在线缓存清理把原图删掉
                        if (image.isFile) {
                            zip.putNextEntry(ZipEntry("$folderName/${image.name}"))
                            image.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            copied++
                        }
                    }
                }
                if (copied > 0) {
                    written++
                }
                onChapterProcessed(index + 1)
            }
        }
        return written
    }

    /** 本章缓存里的图片，文件名 `MAO-001.jpg` 自带页序，原样导出。 */
    internal fun chapterImageFiles(book: Book, chapter: BookChapter): List<File> {
        val chapterDir = BookHelp.getChapterContentFile(book, chapter).parentFile
            ?: return emptyList()
        return File(chapterDir, imageFolderName).listFiles().orEmpty()
            .filter {
                it.isFile && !it.name.endsWith(".tmp") &&
                        it.name.substringAfterLast('.', "").lowercase() in imageExtensions
            }
            .sortedBy { it.name }
    }

    /** 复制一张原图；原图已被缓存预算淘汰时跳过，不留下空文件。 */
    private fun copyImageInto(chapterDir: FileDoc, image: File): Boolean =
        BookHelp.pinImageFile(image).use {
            if (image.isFile) {
                chapterDir.createFileIfNotExist(image.name).openOutputStream().getOrThrow()
                    .use { output ->
                        image.inputStream().use { it.copyTo(output) }
                    }
                true
            } else {
                false
            }
        }

    /** 同名书目录已存在时依次加 ` (2)`、` (3)`，不复用上一次的导出结果。 */
    private fun uniqueFolderName(target: FileDoc, name: String): String {
        val existing = target.list()?.mapTo(hashSetOf()) { it.name }.orEmpty()
        if (name !in existing) {
            return name
        }
        var index = 2
        while ("$name ($index)" in existing) {
            index++
        }
        return "$name ($index)"
    }

    internal fun bookFolderName(book: Book): String {
        val cleaned = book.name
            .replace(AppPattern.fileNameRegex2, " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxNameLength)
        return cleaned.ifEmpty { "未命名" }
    }
}
