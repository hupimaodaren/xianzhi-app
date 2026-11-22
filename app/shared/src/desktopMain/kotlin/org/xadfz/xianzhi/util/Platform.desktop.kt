package org.xadfz.xianzhi.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun fileExists(path: String): Boolean = java.io.File(path).exists()

private val osName: String = System.getProperty("os.name").orEmpty()

/** 桌面端三平台（Windows / macOS / Linux）统一按 os.name 判断，供窗口与文案复用 */
actual val platformName: String = run {
    val family = when {
        osName.startsWith("Mac", ignoreCase = true) -> "macOS"
        osName.startsWith("Windows", ignoreCase = true) -> "Windows"
        osName.startsWith("Linux", ignoreCase = true) -> "Linux"
        osName.isBlank() -> "Desktop"
        else -> osName
    }
    val version = System.getProperty("os.version")?.takeIf { it.isNotBlank() }
    if (version != null) "$family $version" else family
}

actual fun openUrl(url: String) {
    // 优先走 AWT 系统默认浏览器；失败时按平台回退到系统命令
    val ok = runCatching {
        if (java.awt.Desktop.isDesktopSupported() &&
            java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)
        ) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
            true
        } else {
            false
        }
    }.getOrDefault(false)
    if (ok) return
    val command = when {
        osName.startsWith("Mac", ignoreCase = true) -> arrayOf("open", url)
        osName.startsWith("Windows", ignoreCase = true) ->
            arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
        else -> arrayOf("xdg-open", url)
    }
    Runtime.getRuntime().exec(command)
}

actual fun userDataDirPath(): String =
    org.xadfz.xianzhi.settings.DesktopPaths.home().absolutePath

actual fun formatHistoryTime(nowMillis: Long, tsMillis: Long): String {
    val diff = nowMillis - tsMillis
    if (diff < 60_000L) return "刚刚"
    if (diff < 3_600_000L) return "${diff / 60_000L} 分钟前"
    if (diff < 86_400_000L) return "${diff / 3_600_000L} 小时前"
    val now = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
    val ts = java.util.Calendar.getInstance().apply { timeInMillis = tsMillis }
    val md = "${ts.get(java.util.Calendar.MONTH) + 1}月${ts.get(java.util.Calendar.DAY_OF_MONTH)}日"
    return if (now.get(java.util.Calendar.YEAR) == ts.get(java.util.Calendar.YEAR)) md
    else "${ts.get(java.util.Calendar.YEAR)}年$md"
}
