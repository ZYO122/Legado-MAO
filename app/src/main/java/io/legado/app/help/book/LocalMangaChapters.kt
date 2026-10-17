package io.legado.app.help.book

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.utils.AlphanumComparator
import io.legado.app.utils.ArchiveUtils
import io.legado.app.utils.treeChildren
import splitties.init.appCtx
import java.io.File

/**
 * 本地漫画的图片来源与分章规则。
 *
 * 目录导入、zip 导入、书籍详情页的目录、漫画阅读器都走这一套；各处各算一套时，
 * 同一本书在不同入口会出现不同的章节数与章名。
 */
object LocalMangaChapters {

    /** 本地漫画支持的图片扩展名。 */
    val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "avif", "bmp")

    fun isImagePath(path: String): Boolean =
        path.substringAfterLast('.', "").lowercase() in imageExtensions

    /**
     * 图片路径 → 章标题 → 该章图片路径（已按文件名排序，即阅读顺序）。
     *
     * 按父目录分章；每个目录只有一张图说明没分章，整本当一章、章名用 [fallbackTitle]。
     */
    fun group(paths: List<String>, fallbackTitle: String): Map<String, List<String>> {
        val images = paths.filter(::isImagePath)
        if (images.isEmpty()) return emptyMap()
        val sorted = images.sortedWith(AlphanumComparator)
        val grouped = sorted.groupBy { path ->
            path.substringBeforeLast('/', "").takeIf { it.isNotBlank() } ?: fallbackTitle
        }
        return if (grouped.size == sorted.size) mapOf(fallbackTitle to sorted) else grouped
    }

    /** 本地漫画的图片路径：目录（File/SAF）与 zip/cbz 压缩包都支持，只列表不读内容。 */
    fun imagePaths(bookUrl: String, originName: String): List<String> = when {
        isDirectory(bookUrl) -> directoryImagePaths(bookUrl)
        originName.endsWith(".zip", true) || originName.endsWith(".cbz", true) ->
            archiveImageNames(bookUrl)

        else -> emptyList()
    }

    /** 目录下的图片相对路径（`/` 分隔）。 */
    fun directoryImagePaths(bookUrl: String): List<String> {
        if (!bookUrl.startsWith("content://")) {
            val root = File(pathOf(bookUrl))
            if (!root.isDirectory) return emptyList()
            return root.walkTopDown()
                .filter { it.isFile && isImagePath(it.name) }
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .toList()
        }
        return documentImagePaths(bookUrl.toUri())
    }

    /** 压缩包内的图片条目名；只列表，不解压。 */
    fun archiveImageNames(bookUrl: String): List<String> =
        runCatching { ArchiveUtils.getArchiveFilesName(Uri.parse(bookUrl)) }
            .getOrDefault(emptyList())
            .filter(::isImagePath)

    /**
     * 是否是可直接列目录的本地漫画来源。
     *
     * 注意：SAF 的 document uri 形如 `.../tree/<树id>/document/<文档id>`，**也**满足 isTreeUri，
     * 不能只看它判断目录；只有“纯 tree uri”（没有 document 段）才能直接当目录，其余按文档 mime 判。
     */
    fun isDirectory(bookUrl: String): Boolean =
        if (bookUrl.startsWith("content://")) {
            val uri = bookUrl.toUri()
            val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            if (documentId == null && DocumentsContract.isTreeUri(uri)) {
                true
            } else {
                DocumentFile.fromSingleUri(appCtx, uri)?.isDirectory == true
            }
        } else {
            File(pathOf(bookUrl)).isDirectory
        }

    /** `file:` 前缀只是编码差异，取本地路径时统一去掉。 */
    fun pathOf(bookUrl: String): String =
        if (bookUrl.startsWith("file:")) Uri.parse(bookUrl).path.orEmpty() else bookUrl

    private fun documentImagePaths(uri: Uri, prefix: String = ""): List<String> =
        treeChildren(appCtx, uri, DocumentsContract.getDocumentId(uri)).flatMap { child ->
            val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
            when {
                child.isDir -> documentImagePaths(child.uri, path)
                isImagePath(child.name) -> listOf(path)
                else -> emptyList()
            }
        }
}
