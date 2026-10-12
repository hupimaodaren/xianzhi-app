package org.xadfz.xianzhi.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun fileExists(path: String): Boolean = java.io.File(path).exists()

actual val platformName: String = "Android"
