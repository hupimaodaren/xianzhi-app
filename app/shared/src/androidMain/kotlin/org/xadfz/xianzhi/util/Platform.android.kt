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
