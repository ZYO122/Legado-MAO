package io.legado.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.io.File

private const val DEFAULT_SPLASH_ASSET = "file:///android_asset/splash/splash.png"
private const val SPLASH_DURATION_MILLIS = 1500L
private const val SPLASH_FADE_MILLIS = 400

// 与开屏图底色、系统开屏背景色一致，保证「系统开屏 → 自绘」无感切换
private val SplashBackgroundColor = Color(0xFFF9F9F9)

/**
 * 冷启动时盖在主界面之上的自绘开屏：主界面在底下正常加载，本层 [SPLASH_DURATION_MILLIS] 后淡出，
 * 淡出结束回调 [onFinished]。是否展示、以及「每次进程只播一次」由调用方控制。
 */
@Composable
fun SplashAnimationOverlay(
    customPath: String,
    onFinished: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MILLIS)
        visible = false
        delay(SPLASH_FADE_MILLIS.toLong())
        onFinished()
    }
    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = fadeOut(animationSpec = tween(SPLASH_FADE_MILLIS)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SplashBackgroundColor)
        ) {
            AsyncImage(
                model = customPath.takeIf { it.isNotBlank() }?.let { File(it) }
                    ?: DEFAULT_SPLASH_ASSET,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
