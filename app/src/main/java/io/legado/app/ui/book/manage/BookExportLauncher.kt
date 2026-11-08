package io.legado.app.ui.book.manage

import android.content.Context
import android.net.Uri
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.Book
import io.legado.app.help.book.isImage
import io.legado.app.service.ExportBookService
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.startService

/** 导出文件夹缓存在 ACache 里，书架导出与缓存管理共用同一个上次选择。 */
internal const val ExportBookPathCacheKey = "exportBookPath"

/** 导出格式下拉的索引 → 服务认识的后缀；越界时退回第一个格式。 */
internal fun BookshelfManageScreenExportConfig.exportTypeName(types: List<String>): String =
    types.getOrNull(exportType) ?: types.first()

/** 漫画书的缓存按漫画格式导（CBZ / 文件夹），文本书才用下拉里的 txt / epub。 */
internal fun exportTypeFor(book: Book, textExportType: String): String =
    if (book.isImage) ExportBookService.typeManga else textExportType

/** 文件夹选择器返回的 uri → FileDoc 能用的路径，content uri 原样保留。 */
internal fun exportFolderPathOf(uri: Uri): String? =
    if (uri.isContentScheme()) uri.toString() else uri.path

/** 交给 [ExportBookService] 按当前导出设置导出这本书；[cachedOnly] 只导已缓存章节。 */
internal fun startExport(
    context: Context,
    path: String,
    bookUrl: String,
    exportType: String,
    cachedOnly: Boolean = false,
) {
    context.startService<ExportBookService> {
        action = IntentAction.start
        putExtra("bookUrl", bookUrl)
        putExtra("exportType", exportType)
        putExtra("exportPath", path)
        putExtra("cachedOnly", cachedOnly)
    }
}
