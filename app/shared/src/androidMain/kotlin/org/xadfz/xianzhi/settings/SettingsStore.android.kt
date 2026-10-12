package org.xadfz.xianzhi.settings

import android.content.Context

/** Application Context，由 MainActivity 首启注入 */
object AndroidApp {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}

actual fun createSettingsStore(): SettingsStore = object : SettingsStore {
    private val prefs = AndroidApp.context.getSharedPreferences("xianzhi", Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    override fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }
}
