package io.legado.app.ui.book.read.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.model.ReadBook
import io.legado.app.ui.widget.components.alert.ChapterRangeDialog

/**
 * 阅读器里的缓存选择：填起始章/结束章，确认后缓存这个区间。
 */
@Composable
fun DownloadSheet(
    onDismissRequest: () -> Unit,
    onDownload: (start: Int, end: Int) -> Unit,
) {
    val book = ReadBook.book
    ChapterRangeDialog(
        show = true,
        title = stringResource(R.string.cache_selection),
        initialStart = (book?.durChapterIndex ?: 0) + 1,
        initialEnd = book?.totalChapterNum ?: 0,
        onConfirm = { start, end ->
            if (start != null && end != null) {
                onDownload(start, end)
                onDismissRequest()
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
