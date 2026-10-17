package io.legado.app.data.repository.manga

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.domain.model.manga.MangaChapterContent
import io.legado.app.domain.model.manga.MangaPageContent
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.LocalMangaChapters
import io.legado.app.utils.ArchiveUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.treeChildren
import splitties.init.appCtx
import java.io.File

/** Dedicated local comic metadata loader for image directories and comic archives. */
internal class LocalMangaLoader(private val cacheRoot: File) : AutoCloseable {
    private val extractedRoots = linkedSetOf<File>()
    private val extractedBooks = mutableMapOf<String, File>()

    fun supports(book: Book): Boolean = book.originName.endsWith(".cbz", true) ||
            book.originName.endsWith(".zip", true) || LocalMangaChapters.isDirectory(book.bookUrl)

    fun chapters(book: Book): List<BookChapter> =
        imageGroups(book).keys.mapIndexed { index, title ->
            BookChapter(
                url = "local-manga://chapter/$index",
                title = title,
                bookUrl = book.bookUrl,
                index = index,
            )
        }

    fun load(book: Book, chapter: BookChapter): MangaChapterContent {
        val entry = imageGroups(book).entries.elementAtOrNull(chapter.index)
            ?: throw NoStackTraceException("Local comic chapter is missing")
        val urls = entry.value.map(ImageEntry::url)
        return MangaChapterContent(
            chapter.index,
            chapter.title,
            chapter.url,
            urls.mapIndexed { index, url -> MangaPageContent(url, index, urls.size) },
            false,
            chapter.getCacheFolderName(),
        )
    }

    private fun imageGroups(book: Book): Map<String, List<ImageEntry>> {
        val images = if (LocalMangaChapters.isDirectory(book.bookUrl)) {
            directoryImages(book.bookUrl)
        } else {
            archiveImages(book)
        }
        if (images.isEmpty()) throw NoStackTraceException("Local comic contains no images")
        val byPath = images.associateBy(ImageEntry::path)
        return LocalMangaChapters.group(images.map(ImageEntry::path), book.name)
            .mapValues { (_, paths) -> paths.mapNotNull(byPath::get) }
    }

    /**
     * 目录来源：普通路径走 File，SAF 走按 document 子项列（目录导入的 bookUrl 是
     * document uri，直接 fromTreeUri 会退回到 tree 根目录、列出整棵树）。
     */
    private fun directoryImages(bookUrl: String): List<ImageEntry> {
        if (!bookUrl.startsWith("content://")) {
            val root = File(LocalMangaChapters.pathOf(bookUrl))
            if (!root.isDirectory) return emptyList()
            return root.walkTopDown()
                .filter { it.isFile && LocalMangaChapters.isImagePath(it.name) }
                .map {
                    ImageEntry(
                        it.relativeTo(root).invariantSeparatorsPath,
                        it.toURI().toString(),
                    )
                }
                .toList()
        }
        return documentImages(bookUrl.toUri())
    }

    private fun documentImages(uri: Uri, prefix: String = ""): List<ImageEntry> =
        treeChildren(appCtx, uri, DocumentsContract.getDocumentId(uri)).flatMap { child ->
            val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
            when {
                child.isDir -> documentImages(child.uri, path)
                LocalMangaChapters.isImagePath(child.name) ->
                    listOf(ImageEntry(path, child.uri.toString()))

                else -> emptyList()
            }
        }

    private fun archiveImages(book: Book): List<ImageEntry> {
        val root = extractedBooks.getOrPut(book.bookUrl) { extract(book) }
        return root.walkTopDown()
            .filter { it.isFile && LocalMangaChapters.isImagePath(it.name) }
            .map {
                ImageEntry(
                    it.relativeTo(root).invariantSeparatorsPath,
                    it.toURI().toString(),
                )
            }
            .toList()
    }

    private fun extract(book: Book): File {
        val base = File(cacheRoot, MD5Utils.md5Encode16(book.bookUrl)).apply { mkdirs() }
        extractedRoots += base
        val files = ArchiveUtils.deCompress(book.bookUrl, base.path) { path ->
            LocalMangaChapters.isImagePath(path)
        }
        return files.map(File::getParentFile).filterNotNull().reduceOrNull(::commonParent) ?: base
    }

    private fun commonParent(first: File, second: File): File {
        var candidate: File? = first
        while (candidate != null && !second.toPath().startsWith(candidate.toPath())) candidate =
            candidate.parentFile
        return candidate ?: first
    }

    override fun close() {
        extractedRoots.forEach { it.deleteRecursively() }
        extractedRoots.clear()
        extractedBooks.clear()
    }

    private data class ImageEntry(val path: String, val url: String)
}
