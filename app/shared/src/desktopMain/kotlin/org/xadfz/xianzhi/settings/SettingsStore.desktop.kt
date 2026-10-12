package org.xadfz.xianzhi.settings

import java.io.File
import java.util.Properties

actual fun createSettingsStore(): SettingsStore = DesktopSettings

/**
 * 设置存为用户目录下的普通 properties 文件（%APPDATA%\xianzhi\settings.properties）。
 * 不使用 java.util.prefs（Windows 上写注册表），保证安装版/便携版均无注册表写入。
 */
object DesktopSettings : SettingsStore {
    private val file: File = File(DesktopPaths.home(), "settings.properties")
    private val props = Properties()

    init {
        runCatching {
            if (file.exists()) file.inputStream().use { props.load(it) }
        }
    }

    @Synchronized
    private fun persist() {
        runCatching {
            file.parentFile?.mkdirs()
            file.outputStream().use { props.store(it, "xianzhi settings") }
        }
    }

    override fun getString(key: String, default: String): String =
        props.getProperty(key, default)

    override fun putString(key: String, value: String) {
        props.setProperty(key, value)
        persist()
    }

    override fun getInt(key: String, default: Int): Int =
        props.getProperty(key)?.toIntOrNull() ?: default

    override fun putInt(key: String, value: Int) {
        props.setProperty(key, value.toString())
        persist()
    }

    override fun getFloat(key: String, default: Float): Float =
        props.getProperty(key)?.toFloatOrNull() ?: default

    override fun putFloat(key: String, value: Float) {
        props.setProperty(key, value.toString())
        persist()
    }
}
