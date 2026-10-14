package org.xadfz.xianzhi.data

import org.xadfz.xianzhi.db.DbFormat
import org.xadfz.xianzhi.db.openReadOnlyDatabase
import org.xadfz.xianzhi.settings.DesktopPaths
import org.xadfz.xianzhi.settings.createSettingsStore
import java.io.File

/**
 * 桌面数据部署，三种数据来源，按优先级：
 * 1. 数据根目录（-Dxianzhi.home 或 %APPDATA%\xianzhi）下已部署的 data（开发期/旧版升级）；
 * 2. 安装包/便携包内置的 resources/data（随 EXE 安装版、ZIP 便携版分发，直接只读打开，
 *    无需首次运行再复制，便携版解压即用、不向系统目录写数据）；
 * 3. 开发期本地数据工作区（仓库根 data/，首次开发运行时复制到数据根）。
 */
object DesktopDataDeployer {

    fun ensureData(): AppContainer {
        val dataHome = File(DesktopPaths.home(), "data").apply { mkdirs() }
        val packed = findPackedDataDir()
        val devSource = findDevDataDir()

        // 格式版本守卫：数据根里是旧格式库（无 meta 或版本不符）时，
        // 用包内/开发期的新格式库做一次性覆盖升级（约 60MB 复制）
        val homeDb = findDb(dataHome)
        if (homeDb != null && versionOf(homeDb) != DbFormat.FORMAT_VERSION) {
            val upgrade = findUpToDateDb(packed) ?: findUpToDateDb(devSource)
            if (upgrade != null && upgrade.canonicalFile != homeDb.canonicalFile) {
                upgrade.copyTo(homeDb, overwrite = true)
            }
        }

        // 选定数据根：home 已部署 > 包内资源 > 开发期复制
        val dataRoot: File = when {
            findDb(dataHome) != null -> dataHome
            packed != null -> packed
            devSource != null -> copyFromDev(devSource, dataHome)
            else -> null
        } ?: error(
            "未找到数据库。请将 xianzhi.db（或带图版 xianzhi_img.db）放到 $dataHome，" +
                "或使用包含数据的正式分发包。",
        )

        val imageEdition = File(dataRoot, IMG_DB).isFile
        val dbName = if (imageEdition) IMG_DB else TXT_DB
        val dbFile = File(dataRoot, dbName)
        check(dbFile.isFile) { "数据库缺失：$dbFile" }

        val missingDir = File(dataRoot, DIR_MISSING)
        val contentDir = if (imageEdition) File(dataRoot, DIR_CONTENT) else null

        return AppContainer(
            dbPath = dbFile.absolutePath,
            missingImgDir = missingDir.absolutePath,
            contentImgDir = contentDir?.absolutePath,
            settings = createSettingsStore(),
        )
    }

    private const val TXT_DB = "xianzhi.db"
    private const val IMG_DB = "xianzhi_img.db"
    private const val DIR_MISSING = "missing_chars"
    private const val DIR_CONTENT = "content_images"

    private fun findDb(dir: File): File? =
        File(dir, IMG_DB).takeIf { it.isFile } ?: File(dir, TXT_DB).takeIf { it.isFile }

    private fun findUpToDateDb(dir: File?): File? =
        dir?.let { findDb(it) }?.takeIf { versionOf(it) == DbFormat.FORMAT_VERSION }

    /** 打开探测库格式版本；打不开/无 meta 表（v1 旧库）按版本 1 */
    private fun versionOf(file: File): Int {
        val db = runCatching { openReadOnlyDatabase(file.absolutePath) }.getOrNull()
            ?: return 1
        return try {
            DbFormat.readVersion(db)
        } finally {
            db.close()
        }
    }

    /** 包内资源：Compose/jpackage 约定的 resources 目录及可执行文件周边候选 */
    private fun findPackedDataDir(): File? {
        val candidates = mutableListOf<File>()
        System.getProperty("compose.application.resources.dir")
            ?.takeIf { it.isNotBlank() }
            ?.let { candidates += File(it, "data") }
        System.getProperty("jpackage.app-path")
            ?.takeIf { it.isNotBlank() }
            ?.let {
                val exeDir = File(it).parentFile
                candidates += File(exeDir, "app/resources/data")
                candidates += File(exeDir, "resources/data")
                candidates += File(exeDir, "data")
            }
        return candidates.firstOrNull { findDb(it) != null }
    }

    /** 开发期候选：desktopApp 运行工作目录为 app/desktopApp，仓库根在两级之上 */
    private fun findDevDataDir(): File? {
        val candidates = listOf("../../data", "../data", "data", "../../../data")
        return candidates.map { File(it) }.firstOrNull { findDb(it) != null }
    }

    /** 开发期：把 db 与图片从本地数据工作区复制到数据根（只补不覆盖） */
    private fun copyFromDev(source: File, dest: File): File {
        val imgEdition = File(source, IMG_DB).isFile
        File(source, if (imgEdition) IMG_DB else TXT_DB)
            .let { src -> File(dest, src.name).also { if (!it.exists()) src.copyTo(it) } }
        copyDirPreserving(File(source, DIR_MISSING), File(dest, DIR_MISSING))
        if (imgEdition) {
            copyDirPreserving(File(source, DIR_CONTENT), File(dest, DIR_CONTENT))
        }
        return dest
    }

    private fun copyDirPreserving(src: File, dest: File) {
        if (!src.isDirectory) return
        dest.mkdirs()
        src.listFiles()?.forEach { child ->
            val out = File(dest, child.name)
            if (child.isFile && !out.exists()) child.copyTo(out)
        }
    }
}
