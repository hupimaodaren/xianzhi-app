package org.xadfz.xianzhi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 深色：正文 #C8C8C8，背景 #121212（需求 4.3 第 6 条）
private val DarkColors = darkColorScheme(
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onBackground = Color(0xFFC8C8C8),
    onSurface = Color(0xFFC8C8C8),
)

private val LightColors = lightColorScheme()

@Composable
fun XianzhiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
