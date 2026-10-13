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
