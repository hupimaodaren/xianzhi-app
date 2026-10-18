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
            if (v == _fontSize.intValue) return   // 滑杆连续回调时避免无谓写盘
            _fontSize.intValue = v
            store.putInt(SettingsKeys.FONT_SIZE, v)
        }

    private val _lineSpacing = mutableStateOf(store.getFloat(SettingsKeys.LINE_SPACING, 1.5f))
    var lineSpacing: Float
        get() = _lineSpacing.value
        set(value) {
            val v = value.coerceIn(1.2f, 2.0f)
            if (v == _lineSpacing.value) return
            _lineSpacing.value = v
            store.putFloat(SettingsKeys.LINE_SPACING, v)
        }

    private val _themeMode = mutableStateOf(ThemeMode.of(store.getInt(SettingsKeys.THEME_MODE, 0)))
    var themeMode: ThemeMode
        get() = _themeMode.value
        set(value) {
            if (value == _themeMode.value) return
            _themeMode.value = value
            store.putInt(SettingsKeys.THEME_MODE, value.value)
        }

    // 检索阈值：超过软阈值时询问是否继续，达到硬阈值时强制终止；修改即时生效并持久化
    private val _searchSoftLimit = mutableIntStateOf(
        store.getInt(SettingsKeys.SEARCH_SOFT_LIMIT, DEFAULT_SEARCH_SOFT_LIMIT),
    )
    var searchSoftLimit: Int
        get() = _searchSoftLimit.intValue
        set(value) {
            val v = value.coerceIn(SOFT_MIN, SOFT_MAX)
            if (v == _searchSoftLimit.intValue) return
            _searchSoftLimit.intValue = v
            store.putInt(SettingsKeys.SEARCH_SOFT_LIMIT, v)
            if (v > searchHardLimit) searchHardLimit = v // 保证硬阈值不小于软阈值
        }

    private val _searchHardLimit = mutableIntStateOf(
        store.getInt(SettingsKeys.SEARCH_HARD_LIMIT, DEFAULT_SEARCH_HARD_LIMIT),
    )
    var searchHardLimit: Int
        get() = _searchHardLimit.intValue
        set(value) {
            val v = value.coerceIn(searchSoftLimit, HARD_MAX)
            if (v == _searchHardLimit.intValue) return
            _searchHardLimit.intValue = v
            store.putInt(SettingsKeys.SEARCH_HARD_LIMIT, v)
        }

    companion object {
        const val DEFAULT_SEARCH_SOFT_LIMIT = 500
        const val DEFAULT_SEARCH_HARD_LIMIT = 2000
        const val SOFT_MIN = 100
        const val SOFT_MAX = 5000
        const val HARD_MAX = 50000
    }
}

val LocalSettings: ProvidableCompositionLocal<SettingsState?> = compositionLocalOf { null }
