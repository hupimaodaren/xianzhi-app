package org.xadfz.xianzhi.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.util.ioDispatcher

@Composable
actual fun rememberFilePainter(path: String): Painter? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(ioDispatcher) {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    return bitmap?.let { BitmapPainter(it) }
}
