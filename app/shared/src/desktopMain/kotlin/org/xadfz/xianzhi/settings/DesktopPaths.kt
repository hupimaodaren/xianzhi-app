package org.xadfz.xianzhi.settings

import java.io.File

/**
 * 桌面端数据/设置根目录。
 * - 可用 -Dxianzhi.home=... 覆盖（开发期/便携场景）；
 * - 默认 %APPDATA%\xianzhi（无 APPDATA 时退用户主目录）。
 * 便携版的设置也以普通 properties 文件存放，全程不写系统注册表。
 */
object DesktopPaths {
    fun home(): File {
        val override = System.getProperty("xianzhi.home")
        if (!override.isNullOrBlank()) return File(override)
        val base = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("user.home")
        return File(base, "xianzhi")
    }
}
