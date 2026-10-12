package org.xadfz.xianzhi.data

import org.xadfz.xianzhi.settings.createSettingsStore
import java.io.File

/**
 * 桌面数据部署：数据目录 %APPDATA%\xianzhi\data。
 * 首次运行时从工程「数据/」目录（开发期）或安装包资源目录复制 db 与图片，之后只读打开。
 */
object DesktopDataDeployer {

    fun ensureData(): AppContainer {
        val appRoot = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "xianzhi")
        val dataDir = File(appRoot, "data").apply { mkdirs() }

        val source = findDevDataDir()

        val imageEdition = source?.let { File(it, "xianzhi_img.db").exists() } ?: false
        val dbName = if (imageEdition) "xianzhi_img.db" else "xianzhi.db"
        val dbFile = File(dataDir, dbName)
        if (!dbFile.exists()) {
            val srcDb = source?.let { File(it, dbName) }
            require(srcDb != null && srcDb.exists()) {
                "未找到 $dbName。请将数据库放到 $dataDir（见仓库 README「数据获取」）。"
            }
            srcDb.copyTo(dbFile)
        }

        val missingDir = File(dataDir, DIR_MISSING)
        if (!missingDir.exists()) {
            source?.let { copyDirPreserving(File(it, DIR_MISSING), missingDir) }
        }

        var contentDir: File? = null
        if (imageEdition) {
            contentDir = File(dataDir, DIR_CONTENT)
            if (!contentDir.exists()) {
                source?.let { copyDirPreserving(File(it, DIR_CONTENT), contentDir) }
            }
        }

        return AppContainer(
            dbPath = dbFile.absolutePath,
            missingImgDir = missingDir.absolutePath,
            contentImgDir = contentDir?.absolutePath,
            settings = createSettingsStore(),
        )
    }

    private const val DIR_MISSING = "缺字图"
    private const val DIR_CONTENT = "配图"

    /** 开发期候选：desktopApp 运行工作目录为 app/desktopApp，仓库根在两级之上 */
    private fun findDevDataDir(): File? {
        val candidates = listOf("../../数据", "../数据", "数据", "../../../数据")
        return candidates.map { File(it) }.firstOrNull { File(it, "xianzhi.db").exists() }
    }

    /** 仅复制目标目录中尚不存在的文件（断点补齐，不覆盖已有数据） */
    private fun copyDirPreserving(src: File, dest: File) {
        if (!src.isDirectory) return
        dest.mkdirs()
        src.listFiles()?.forEach { child ->
            val out = File(dest, child.name)
            if (child.isFile && !out.exists()) child.copyTo(out)
        }
    }
}
