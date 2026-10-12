package org.xadfz.xianzhi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.xadfz.xianzhi.util.ioDispatcher
import java.io.File

@Composable
actual fun rememberFilePainter(path: String): Painter? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(ioDispatcher) {
            val f = File(path)
            if (!f.exists()) null
            else runCatching { Image.makeFromEncoded(f.readBytes()).toComposeImageBitmap() }.getOrNull()
        }
    }
    return bitmap?.let { BitmapPainter(it) }
}
