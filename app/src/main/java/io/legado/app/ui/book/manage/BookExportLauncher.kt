package io.legado.app.ui.book.manage

import android.content.Context
import android.net.Uri
import io.legado.app.constant.IntentAction
import io.legado.app.service.ExportBookService
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.startService

/** 导出文件夹缓存在 ACache 里，书架导出与缓存管理共用同一个上次选择。 */
internal const val ExportBookPathCacheKey = "exportBookPath"

/** 导出格式下拉的索引 → 服务认识的后缀；越界时退回第一个格式。 */
internal fun BookshelfManageScreenExportConfig.exportTypeName(types: List<String>): String =
    types.getOrNull(exportType) ?: types.first()

/** 文件夹选择器返回的 uri → FileDoc 能用的路径，content uri 原样保留。 */
internal fun exportFolderPathOf(uri: Uri): String? =
    if (uri.isContentScheme()) uri.toString() else uri.path

/** 交给 [ExportBookService] 按当前导出设置导出这本书。 */
internal fun startExport(
    context: Context,
    path: String,
    bookUrl: String,
    exportType: String,
) {
    context.startService<ExportBookService> {
        action = IntentAction.start
        putExtra("bookUrl", bookUrl)
        putExtra("exportType", exportType)
        putExtra("exportPath", path)
    }
}
