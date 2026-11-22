package org.xadfz.xianzhi.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.data.DesktopDataDeployer
import org.xadfz.xianzhi.settings.SettingsKeys
import org.xadfz.xianzhi.ui.XianzhiApp
import java.io.File

/**
 * 无窗口界面截图工具（仅开发期使用）。
 *
 * 直接构造 ImageComposeScene 把界面渲染到离屏位图再写出 PNG：不需要真实窗口与
 * 显示环境，可在无桌面会话的机器上批量产出界面截图，用于设计评审与迭代前后对比。
 *
 * 用法：gradle :desktopApp:screenshot
 * 产物：desktopApp/build/screenshots 目录下的 png 文件
 */
private data class Shot(
    val name: String,
    val tab: Int = 0,
    val cat: String? = null,
    val book: String? = null,
    val article: Long? = null,
    val query: String = "",
    val history: Boolean = false,
)

fun main() {
    val outDir = File(System.getProperty("shots.out") ?: "build/screenshots").apply { mkdirs() }
    val width = System.getProperty("shots.width")?.toIntOrNull() ?: 1264
    val height = System.getProperty("shots.height")?.toIntOrNull() ?: 792
    val only = System.getProperty("shots.only").orEmpty()

    // 离屏渲染没有系统主题信息，isSystemInDarkTheme() 恒为 true；
    // 截图固定用浅色宣纸主题（与产品主视觉一致），保证前后对比基线稳定。
    val themeMode = when (System.getProperty("shots.theme").orEmpty()) {
        "dark" -> org.xadfz.xianzhi.ui.ThemeMode.DARK
        "system" -> null
        else -> org.xadfz.xianzhi.ui.ThemeMode.LIGHT
    }
    if (themeMode != null) {
        org.xadfz.xianzhi.settings.createSettingsStore()
            .putInt(SettingsKeys.THEME_MODE, themeMode.value)
    }

    val container = DesktopDataDeployer.ensureData()
    val categories = container.queries.categories()
    val firstCat = categories.firstOrNull()?.name
    val firstBook = categories.firstOrNull()?.books?.firstOrNull()
    // 阅读页选一篇「像书页」的文章做样张：正文是成段散文（不是表格排版），
    // 并恰好带一张整段配图，长度约一到两屏。逐条打分挑最像正式志书正文的一篇，
    // 避免截到封面、目录或表格页。
    // 允许用 -Pshots.article=<篇目 id> 指定样张，便于复现某张固定截图
    // 前置页（封面、序、凡例、目录等）版式特殊，不适合当正文样张
    val frontMatter = setOf(
        "封面", "扉页", "书名页", "版权页", "凡例", "序", "序一", "序二", "序三",
        "目录", "编后记", "后记", "说明", "出版说明", "编纂说明", "本卷编辑说明",
    )
    val richArticle = System.getProperty("shots.article")?.toLongOrNull() ?: run {
        val imageMarker = "⟦img:"
        val books = container.queries.booksOfCategory(firstCat.orEmpty())
        var bestId: Long? = null
        var bestScore = Int.MIN_VALUE
        var longest: Pair<Long, Int>? = null
        for (b in books) {
            for (a in container.queries.articlesOfBook(b.id).take(80)) {
                if (a.title.trim().replace("　", "") in frontMatter) continue
                val body = container.queries.articleDetail(a.id)?.body.orEmpty()
                if (body.isBlank()) continue
                val cur = longest
                if (cur == null || body.length > cur.second) longest = a.id to body.length
                if (body.length !in 600..1600) continue
                val paragraphs = body.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                // 表格页由大量短行（单元、框线）构成，正文页几乎每行都是成段长句
                val shortLines = paragraphs.count { it.length < 25 }
                if (shortLines > 2) continue
                val prose = paragraphs.count { it.length > 60 && !it.contains(imageMarker) }
                val soleImage = paragraphs.count { it.startsWith(imageMarker) && it.indexOf(imageMarker, 3) < 0 }
                if (prose < 4) continue
                val score = prose * 10 + (if (soleImage == 1) 60 else 0) - kotlin.math.abs(body.length - 1200) / 50
                if (score > bestScore) { bestScore = score; bestId = a.id }
            }
        }
        bestId ?: longest?.first
    }

    val shots = listOf(
        Shot("books"),
        Shot("booklist", cat = firstCat),
        Shot("toc", cat = firstCat, book = firstBook?.id),
        Shot("search", tab = 1, query = "关中书院"),
        Shot("favorites", tab = 2),
        Shot("history", tab = 3, history = true),
        Shot("settings", tab = 3),
        Shot("reader", tab = 0, cat = firstCat, book = firstBook?.id, article = richArticle),
    ).filter { only.isBlank() || it.name == only }

    for (s in shots) {
        val started = System.currentTimeMillis()
        val file = File(outDir, s.name + ".png")
        renderTo(container, s, width, height, file)
        println("已写出 " + file.absolutePath + " （" + (System.currentTimeMillis() - started) + "ms）")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun renderTo(container: AppContainer, s: Shot, width: Int, height: Int, out: File) {
    val content: @Composable () -> Unit = {
        XianzhiApp(
            container = container,
            initialTab = s.tab,
            initialCat = s.cat,
            initialBookId = s.book,
            initialBookTitle = "",
            initialArticleId = s.article,
            initialQuery = s.query,
            initialHistory = s.history,
        )
    }
    val scene = ImageComposeScene(
        width = width,
        height = height,
        density = Density(1f, 1f),
        content = content,
    )
    try {
        // 数据查询走 IO 线程：多渲染若干帧，等异步结果回填后再取图
        var last: org.jetbrains.skia.Image? = null
        repeat(24) {
            last = scene.render(System.nanoTime())
            Thread.sleep(60)
        }
        out.writeBytes(
            last!!.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes,
        )
    } finally {
        scene.close()
    }
}
