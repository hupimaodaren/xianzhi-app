package org.xadfz.xianzhi.util

import kotlinx.coroutines.CoroutineDispatcher

/** IO 调度器（Android/JVM 均为 Dispatchers.IO） */
expect val ioDispatcher: CoroutineDispatcher

/** 判断绝对路径文件是否存在 */
expect fun fileExists(path: String): Boolean

/** 运行平台名（关于页显示） */
expect val platformName: String

/** 用系统默认浏览器打开网址（关于页的 GitHub 官方渠道链接） */
expect fun openUrl(url: String)

/** 用户可写数据目录（收藏库等用户数据存放位置） */
expect fun userDataDirPath(): String
