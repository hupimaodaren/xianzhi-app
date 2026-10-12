package org.xadfz.xianzhi.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.xadfz.xianzhi.settings.AndroidApp
import org.xadfz.xianzhi.settings.createSettingsStore
import java.io.File

/**
 * 首启数据部署：db 随 APK 打包在 assets，首次启动流式复制到 filesDir/data
 * （174MB，显示进度），之后只读打开。缺字图/配图同样从 assets 补齐。
 */
object AndroidDataDeployer {
    /** -1=进行中（不确定进度），1=完成 */
    var progress by mutableStateOf(-1f)
        private set
    var message by mutableStateOf("准备数据…")
        private set

    fun ensureData(): AppContainer {
        val ctx = AndroidApp.context
        val dir = File(ctx.filesDir, "data").apply { mkdirs() }

        // assets 同时只有一个 db：文字版 xianzhi.db / 带图版 xianzhi_img.db
        val imageEdition = assetExists(ctx, ASSET_IMG_DB)
        val dbName = if (imageEdition) ASSET_IMG_DB else ASSET_DB
        val dbFile = File(dir, dbName)
        if (!dbFile.exists()) {
            message = "正在复制数据库（首次启动约需 1 分钟）…"
            copyAssetFile(ctx, dbName, dbFile)
        }

        val missingDir = File(dir, DIR_MISSING)
        if (!missingDir.exists()) {
            message = "正在复制缺字图…"
            copyAssetDir(ctx, ASSET_DIR_MISSING, missingDir)
        }

        var contentDir: File? = null
        if (imageEdition) {
            contentDir = File(dir, DIR_CONTENT)
            if (!contentDir.exists()) {
                message = "正在复制配图…"
                copyAssetDir(ctx, ASSET_DIR_CONTENT, contentDir)
            }
        }

        progress = 1f
        return AppContainer(
            dbPath = dbFile.absolutePath,
            missingImgDir = missingDir.absolutePath,
            contentImgDir = contentDir?.absolutePath,
            settings = createSettingsStore(),
        )
    }

    private const val ASSET_DB = "xianzhi.db"
    private const val ASSET_IMG_DB = "xianzhi_img.db"

    // assets 内必须用 ASCII 目录名（aapt2 编码限制）；复制到 filesDir 后仍用 ASCII 名
    private const val ASSET_DIR_MISSING = "missing_chars"
    private const val ASSET_DIR_CONTENT = "content_images"
    private const val DIR_MISSING = "missing_chars"
    private const val DIR_CONTENT = "content_images"

    private fun assetExists(ctx: Context, name: String): Boolean =
        runCatching { ctx.assets.open(name).use { } }.isSuccess

    private fun copyAssetFile(ctx: Context, asset: String, dest: File) {
        dest.parentFile?.mkdirs()
        ctx.assets.open(asset).use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                }
            }
        }
    }

    private fun copyAssetDir(ctx: Context, assetDir: String, destDir: File) {
        val names = ctx.assets.list(assetDir) ?: return
        destDir.mkdirs()
        names.forEachIndexed { i, name ->
            val out = File(destDir, name)
            if (!out.exists()) copyAssetFile(ctx, "$assetDir/$name", out)
            progress = (i + 1).toFloat() / names.size
        }
    }
}
