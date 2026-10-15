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

/**
 * 阅读历史的相对时间显示：
 * 1 分钟内「刚刚」，1 小时内「N 分钟前」，24 小时内「N 小时前」，
 * 同年「M月d日」，跨年「yyyy年M月d日」；未来时间（时钟误差）按「刚刚」处理。
 */
expect fun formatHistoryTime(nowMillis: Long, tsMillis: Long): String
