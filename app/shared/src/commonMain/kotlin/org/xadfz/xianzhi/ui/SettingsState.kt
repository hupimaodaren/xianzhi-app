package org.xadfz.xianzhi.ui

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.xadfz.xianzhi.settings.SettingsKeys
import org.xadfz.xianzhi.settings.SettingsStore

/** 主题模式 */
enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2);

    companion object {
        fun of(v: Int): ThemeMode = entries.firstOrNull { it.value == v } ?: SYSTEM
    }
}

/** 可观察的阅读/外观设置，修改即时生效并持久化 */
class SettingsState(private val store: SettingsStore) {
    var fontSize by mutableStateOf(store.getInt(SettingsKeys.FONT_SIZE, 16))
        private set
    var lineSpacing by mutableStateOf(store.getFloat(SettingsKeys.LINE_SPACING, 1.5f))
        private set
    var themeMode by mutableStateOf(ThemeMode.of(store.getInt(SettingsKeys.THEME_MODE, 0)))
        private set

    fun setFontSize(value: Int) {
        fontSize = value.coerceIn(14, 28)
        store.putInt(SettingsKeys.FONT_SIZE, fontSize)
    }

    fun setLineSpacing(value: Float) {
        lineSpacing = value.coerceIn(1.2f, 2.0f)
        store.putFloat(SettingsKeys.LINE_SPACING, lineSpacing)
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
        store.putInt(SettingsKeys.THEME_MODE, mode.value)
    }
}

val LocalSettings: CompositionLocal<SettingsState?> = compositionLocalOf { null }
