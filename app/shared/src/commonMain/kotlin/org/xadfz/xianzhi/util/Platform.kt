package org.xadfz.xianzhi.util

import kotlinx.coroutines.CoroutineDispatcher

/** IO 调度器（Android/JVM 均为 Dispatchers.IO） */
expect val ioDispatcher: CoroutineDispatcher

/** 判断绝对路径文件是否存在 */
expect fun fileExists(path: String): Boolean

/** 运行平台名（关于页显示） */
expect val platformName: String
