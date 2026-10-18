package org.xadfz.xianzhi.settings

import java.io.File
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

actual fun createSettingsStore(): SettingsStore = DesktopSettings

/**
 * 设置存为用户目录下的普通 properties 文件（%APPDATA%\xianzhi\settings.properties）。
 * 不使用 java.util.prefs（Windows 上写注册表），保证安装版/便携版均无注册表写入。
 *
 * 写入策略：内存中的 [props] 立即更新（读取即时生效），落盘在后台线程合并
 * 短时间内的连续改动后异步完成。拖动字号/行距滑杆、离开阅读页写阅读位置
 * 这类高频改动不再阻塞 UI 线程做整文件写入。
 */
object DesktopSettings : SettingsStore {
    private val file: File = File(DesktopPaths.home(), "settings.properties")
    private val props = Properties()

    /** 保护 props 读写（仅在内存操作期间持锁，不覆盖文件 IO） */
    private val lock = Any()

    /** 串行落盘锁：保证任何时刻只有一个线程在写该文件 */
    private val writeLock = Any()

    private val writer = Executors.newSingleThreadExecutor { r ->
        Thread(r, "xianzhi-settings-writer").apply { isDaemon = true }
    }

    /** 是否已有一个「等待合并写入」的任务在排队 */
    private val pending = AtomicBoolean(false)

    init {
        runCatching {
            if (file.exists()) file.inputStream().use { props.load(it) }
        }
        // JVM 正常退出时兜底落盘，避免最后一次改动因合并延迟而丢失
        runCatching {
            Runtime.getRuntime().addShutdownHook(Thread { persist() })
        }
    }

    /** 取当前设置快照并落盘；调用方不必持锁 */
    private fun persist() {
        val snapshot = synchronized(lock) { Properties().apply { putAll(props) } }
        synchronized(writeLock) {
            runCatching {
                file.parentFile?.mkdirs()
                file.outputStream().use { snapshot.store(it, "xianzhi settings") }
            }
        }
    }

    /**
     * 安排一次异步写入：先复位 [pending] 再落盘，因此落盘期间到达的新改动
     * 会另排一次任务，最后一次改动不会丢。
     */
    private fun schedulePersist() {
        if (!pending.compareAndSet(false, true)) return
        writer.execute {
            try {
                Thread.sleep(PERSIST_COALESCE_MS)
            } catch (_: InterruptedException) {
                // 被中断时也照常落盘一次
            }
            pending.set(false)
            persist()
        }
    }

    private fun put(key: String, value: String) {
        synchronized(lock) { props.setProperty(key, value) }
        schedulePersist()
    }

    override fun getString(key: String, default: String): String =
        props.getProperty(key, default)

    override fun putString(key: String, value: String) = put(key, value)

    override fun getInt(key: String, default: Int): Int =
        props.getProperty(key)?.toIntOrNull() ?: default

    override fun putInt(key: String, value: Int) = put(key, value.toString())

    override fun getFloat(key: String, default: Float): Float =
        props.getProperty(key)?.toFloatOrNull() ?: default

    override fun putFloat(key: String, value: Float) = put(key, value.toString())

    /** 连续改动的合并窗口（毫秒） */
    private const val PERSIST_COALESCE_MS = 400L
}
