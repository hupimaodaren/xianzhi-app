package org.xadfz.xianzhi.settings

import java.util.prefs.Preferences

actual fun createSettingsStore(): SettingsStore = DesktopSettings

object DesktopSettings : SettingsStore {
    private val prefs = Preferences.userRoot().node("org/xadfz/xianzhi")

    override fun getString(key: String, default: String): String = prefs.get(key, default)

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
        prefs.flush()
    }

    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    override fun putFloat(key: String, value: Float) {
        prefs.putFloat(key, value)
        prefs.flush()
    }
}
