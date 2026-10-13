package org.xadfz.xianzhi.settings

/**
 * 设置存储（字号/行距/主题/阅读位置/搜索历史）。
 * actual：Android SharedPreferences；Desktop java.util.prefs.Preferences。
 */
interface SettingsStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getFloat(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
}

expect fun createSettingsStore(): SettingsStore

/** 设置键 */
object SettingsKeys {
    const val FONT_SIZE = "font_size"        // 14..28，默认 16
    const val LINE_SPACING = "line_spacing"   // 1.2..2.0，默认 1.5
    const val THEME_MODE = "theme_mode"       // 0 跟随系统 / 1 浅色 / 2 深色
    const val SEARCH_HISTORY = "search_history"
    const val SEARCH_SOFT_LIMIT = "search_soft_limit"  // 100..5000，默认 500
    const val SEARCH_HARD_LIMIT = "search_hard_limit"  // soft..50000，默认 2000

    fun readerIndex(articleId: Long) = "reader:idx:$articleId"
    fun readerOffset(articleId: Long) = "reader:off:$articleId"
}

/** 搜索历史（最近 20 条，换行分隔，标题本身不含换行） */
class SearchHistory(private val store: SettingsStore) {
    fun list(): List<String> =
        store.getString(SettingsKeys.SEARCH_HISTORY, "").split("\n").filter { it.isNotBlank() }

    fun add(keyword: String) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        val updated = (listOf(kw) + list()).distinct().take(20)
        store.putString(SettingsKeys.SEARCH_HISTORY, updated.joinToString("\n"))
    }

    fun clear() = store.putString(SettingsKeys.SEARCH_HISTORY, "")
}
