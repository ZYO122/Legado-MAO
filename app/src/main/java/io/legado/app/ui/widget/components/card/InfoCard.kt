package io.legado.app.ui.widget.components.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.button.series.SmallPlainButton
import io.legado.app.ui.widget.components.icon.AppIcons
import io.legado.app.ui.widget.components.text.AppText

/**
 * 说明卡片：一句说明 + 一个可选的小按钮（默认「关闭」）。
 *
 * 独立提示与分组内说明共用这一套排版，圆角、内边距、文字样式与按钮都可替换；需要“点开看详情”
 * 时用 [InfoNote]。
 */
@Composable
fun InfoCard(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LegadoTheme.typography.labelLargeEmphasized,
    textColor: Color = Color.Unspecified,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color = LegadoTheme.colorScheme.surfaceContainerLow,
    /** 为 null 时不渲染按钮。 */
    actionIcon: ImageVector? = AppIcons.Close,
    actionDescription: String? = null,
    actionSelected: Boolean = false,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
) {
    GlassCard(
        containerColor = containerColor,
        cornerRadius = cornerRadius,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding)
        ) {
            AppText(
                text = text,
                style = textStyle,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            if (onAction != null && actionIcon != null) SmallPlainButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                selected = actionSelected,
                enabled = actionEnabled,
                onClick = onAction
            )
        }
    }
}

/**
 * 分组内顶部的说明：次要文字 + Info 按钮，点开用对话框展示 [detail]。
 *
 * 容器与同组的设置项一致（圆角 4dp、surfaceContainerLow），排成一张同宽卡片；
 * [detail] 用空行分段，`**标签**` 渲染为加粗。
 */
@Composable
fun InfoNote(
    text: String,
    detail: String,
    detailTitle: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LegadoTheme.typography.bodySmall,
    textColor: Color = LegadoTheme.colorScheme.onSurfaceVariant,
) {
    var showDetail by rememberSaveable { mutableStateOf(false) }
    GlassCard(
        modifier = modifier,
        cornerRadius = 4.dp,
        containerColor = LegadoTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            AppText(
                text = text,
                style = textStyle,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            SmallPlainButton(
                icon = AppIcons.Info,
                contentDescription = stringResource(R.string.manga_note_details_of, detailTitle),
                onClick = { showDetail = true }
            )
        }
    }
    AppAlertDialog(
        show = showDetail,
        onDismissRequest = { showDetail = false },
        title = detailTitle,
        confirmText = stringResource(R.string.close),
        onConfirm = { showDetail = false },
        content = {
            detail.split("\n\n").filter { it.isNotBlank() }.forEachIndexed { index, block ->
                AppText(
                    markedBold(block),
                    style = LegadoTheme.typography.bodyMedium,
                    modifier = if (index == 0) Modifier else Modifier.padding(top = 12.dp)
                )
            }
        }
    )
}

/** Renders the `**lead-in**` markers used by the group notes as bold, keeping the rest regular. */
private fun markedBold(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (true) {
        val open = text.indexOf("**", cursor)
        val close = if (open < 0) -1 else text.indexOf("**", open + 2)
        if (open < 0 || close < 0) {
            append(text.substring(cursor))
            return@buildAnnotatedString
        }
        append(text.substring(cursor, open))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(open + 2, close)) }
        cursor = close + 2
    }
}
