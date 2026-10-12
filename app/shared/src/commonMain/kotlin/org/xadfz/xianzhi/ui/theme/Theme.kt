package org.xadfz.xianzhi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 视觉基调：宣纸 · 墨 · 朱砂
 * - 浅色：暖宣底、墨色正文、朱砂印章红作为唯一强调色
 * - 深色：暖调近黑（需求 4.3：背景 #121212 系、正文 #C8C8C8 系），朱砂降饱和
 * - 标题使用衬线体（Android 为宋/楷系，桌面为系统衬线），正文用默认无衬线
 */

// ---------- 浅色：宣纸 ----------
private val Vermilion = Color(0xFF9C3B2A) // 朱砂
private val Paper = Color(0xFFFAF6EC) // 宣纸底
private val PaperHigh = Color(0xFFFFFDF7) // 卡片/表面
private val Ink = Color(0xFF2A241D) // 墨
private val InkSoft = Color(0xFF6F6556) // 淡墨

private val LightColors = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color(0xFFFFF7EE),
    primaryContainer = Color(0xFFF4E1D2),
    onPrimaryContainer = Color(0xFF5A2417),
    secondary = Color(0xFF7C6A55),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE7D7),
    onSecondaryContainer = Color(0xFF43382A),
    background = Paper,
    onBackground = Ink,
    surface = PaperHigh,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EAD9),
    onSurfaceVariant = InkSoft,
    surfaceContainer = Color(0xFFF6F0E3),
    surfaceContainerHigh = Color(0xFFF1E9D8),
    outline = Color(0xFFD6CCB8),
    outlineVariant = Color(0xFFE7DFCE),
    error = Color(0xFFB3261E),
)

// ---------- 深色：夜读 ----------
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD98A76), // 降饱和朱砂
    onPrimary = Color(0xFF401910),
    primaryContainer = Color(0xFF4A2B23),
    onPrimaryContainer = Color(0xFFF2D3C7),
    secondary = Color(0xFFC7B89E),
    onSecondary = Color(0xFF2E2719),
    secondaryContainer = Color(0xFF3A3327),
    onSecondaryContainer = Color(0xFFE5DAC2),
    background = Color(0xFF131210),
    onBackground = Color(0xFFC8C0B2),
    surface = Color(0xFF1A1815),
    onSurface = Color(0xFFC8C0B2),
    surfaceVariant = Color(0xFF2A2620),
    onSurfaceVariant = Color(0xFFA89E8D),
    surfaceContainer = Color(0xFF211E1A),
    surfaceContainerHigh = Color(0xFF2B2722),
    outline = Color(0xFF4A443A),
    outlineVariant = Color(0xFF35302A),
    error = Color(0xFFF2B8B5),
)

private val Serif = FontFamily.Serif

private val XianzhiTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

private val XianzhiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

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
        typography = XianzhiTypography,
        shapes = XianzhiShapes,
        content = content,
    )
}

/**
 * 宽屏阅读舒适边距：正文/列表内容最大宽度 [maxContent]，
 * 超出后左右自动留白并居中（桌面大窗、平板横屏适配）。
 * 需在 BoxWithConstraints 作用域内调用（双端通用）。
 */
fun BoxWithConstraintsScope.comfortableSidePadding(
    maxContent: androidx.compose.ui.unit.Dp = 800.dp,
    min: androidx.compose.ui.unit.Dp = 16.dp,
): androidx.compose.ui.unit.Dp =
    if (maxWidth > maxContent + min * 2) {
        ((maxWidth - maxContent) / 2).coerceAtLeast(min)
    } else {
        min
    }

/** 宽屏判定阈值：达到后用 NavigationRail 替代底部栏 */
val RailBreakpoint = 860.dp
