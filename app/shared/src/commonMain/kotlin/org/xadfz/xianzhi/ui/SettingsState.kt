package org.xadfz.xianzhi.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    private val _fontSize = mutableIntStateOf(store.getInt(SettingsKeys.FONT_SIZE, 16))
    var fontSize: Int
        get() = _fontSize.intValue
        set(value) {
            val v = value.coerceIn(14, 28)
            _fontSize.intValue = v
            store.putInt(SettingsKeys.FONT_SIZE, v)
        }

    private val _lineSpacing = mutableStateOf(store.getFloat(SettingsKeys.LINE_SPACING, 1.5f))
    var lineSpacing: Float
        get() = _lineSpacing.value
        set(value) {
            val v = value.coerceIn(1.2f, 2.0f)
            _lineSpacing.value = v
            store.putFloat(SettingsKeys.LINE_SPACING, v)
        }

    private val _themeMode = mutableStateOf(ThemeMode.of(store.getInt(SettingsKeys.THEME_MODE, 0)))
    var themeMode: ThemeMode
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
            store.putInt(SettingsKeys.THEME_MODE, value.value)
        }
}

val LocalSettings: ProvidableCompositionLocal<SettingsState?> = compositionLocalOf { null }
