package io.legado.app.ui.widget.components.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppTextField
import io.legado.app.ui.widget.components.text.AppText

/**
 * 起始章/结束章双输入对话框，章号按目录里的序号填（从 1 开始）。
 *
 * [allowUnlimited] 为 true 时留空表示该端不限（导出「缓存目录」用）；
 * 为 false 时两端必填且必须「起始 ≤ 结束」（阅读器与缓存管理的缓存动作），
 * 非法时确定按钮不可点，而不是点了才弹一句提示。
 */
@Composable
fun ChapterRangeDialog(
    show: Boolean,
    title: String,
    initialStart: Int?,
    initialEnd: Int?,
    onConfirm: (start: Int?, end: Int?) -> Unit,
    onDismissRequest: () -> Unit,
    allowUnlimited: Boolean = false,
    confirmText: String = stringResource(R.string.ok),
) {
    var startText by remember(show, initialStart, initialEnd) {
        mutableStateOf(initialStart?.toString().orEmpty())
    }
    var endText by remember(show, initialStart, initialEnd) {
        mutableStateOf(initialEnd?.toString().orEmpty())
    }
    val range = parseChapterRange(startText, endText, allowUnlimited)
    val startLabel = stringResource(R.string.start_chapter)
    val endLabel = stringResource(R.string.end_chapter)
    val unlimitedHint = stringResource(R.string.chapter_range_unlimited_hint)
    val invalidHint = stringResource(R.string.chapter_range_invalid)

    AppAlertDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        confirmText = confirmText,
        onConfirm = { range?.let { onConfirm(it.first, it.second) } },
        confirmEnabled = range != null,
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismissRequest,
        titleCenter = true,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        modifier = Modifier.weight(1f),
                        label = startLabel,
                        singleLine = true,
                        isError = range == null && startText.isNotBlank(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    AppTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        modifier = Modifier.weight(1f),
                        label = endLabel,
                        singleLine = true,
                        isError = range == null && endText.isNotBlank(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                if (range == null) {
                    AppText(
                        text = invalidHint,
                        modifier = Modifier.padding(top = 8.dp),
                        style = LegadoTheme.typography.labelSmall,
                        color = LegadoTheme.colorScheme.error,
                    )
                } else if (allowUnlimited) {
                    AppText(
                        text = unlimitedHint,
                        modifier = Modifier.padding(top = 8.dp),
                        style = LegadoTheme.typography.labelSmall,
                        color = LegadoTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/** 解析结果：null 表示输入非法；两端都可能是 null（不限）。 */
private fun parseChapterRange(
    startText: String,
    endText: String,
    allowUnlimited: Boolean,
): Pair<Int?, Int?>? {
    if (!allowUnlimited && (startText.isBlank() || endText.isBlank())) return null
    val start = if (startText.isBlank()) null else startText.trim().toIntOrNull() ?: return null
    val end = if (endText.isBlank()) null else endText.trim().toIntOrNull() ?: return null
    if (start != null && start < 1) return null
    if (end != null && end < 1) return null
    if (start != null && end != null && start > end) return null
    return start to end
}
