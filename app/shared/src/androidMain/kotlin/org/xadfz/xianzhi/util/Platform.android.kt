package org.xadfz.xianzhi.util

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.xadfz.xianzhi.settings.AndroidApp

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun fileExists(path: String): Boolean = java.io.File(path).exists()

actual val platformName: String = "Android"

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    AndroidApp.context.startActivity(intent)
}

actual fun userDataDirPath(): String = AndroidApp.context.filesDir.absolutePath

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
