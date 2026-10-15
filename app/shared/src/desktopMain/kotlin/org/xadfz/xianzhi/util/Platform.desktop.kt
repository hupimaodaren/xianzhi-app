package org.xadfz.xianzhi.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun fileExists(path: String): Boolean = java.io.File(path).exists()

actual val platformName: String =
    "Windows" + (System.getProperty("os.version")?.takeIf { it.isNotBlank() }?.let { " $it" } ?: "")

actual fun openUrl(url: String) {
    // Windows 用系统默认浏览器打开；AWT Desktop 失败时回退 rundll32
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
    if (!ok) {
        Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
    }
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
