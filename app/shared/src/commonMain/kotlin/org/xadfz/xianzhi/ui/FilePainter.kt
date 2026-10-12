package org.xadfz.xianzhi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/** 按绝对路径加载图片为 Painter；文件不存在或解码失败返回 null（两端 actual） */
@Composable
expect fun rememberFilePainter(path: String): Painter?
