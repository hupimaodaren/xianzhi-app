package org.xadfz.xianzhi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.data.DesktopDataDeployer
import org.xadfz.xianzhi.ui.XianzhiApp

fun main() = application {
    var container by remember { mutableStateOf<AppContainer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { DesktopDataDeployer.ensureData() }
        }.onSuccess { container = it }
            .onFailure { error = it.message ?: it.toString() }
    }

    val startTab = System.getenv("XIANZHI_TAB")?.toIntOrNull() ?: 0
    val articleArg = System.getenv("XIANZHI_ARTICLE")
    val startArticle = articleArg?.toLongOrNull()
    val catArg = System.getenv("XIANZHI_CAT")
    val bookArg = System.getenv("XIANZHI_BOOK")
    val winW = System.getenv("XIANZHI_WIDTH")?.toIntOrNull() ?: 1024
    val winH = System.getenv("XIANZHI_HEIGHT")?.toIntOrNull() ?: 768

    Window(
        onCloseRequest = ::exitApplication,
        title = "西安地方志",
        state = rememberWindowState(size = DpSize(winW.dp, winH.dp)),
    ) {
        val ready = container
        // 仅用于非 JBR JDK + 非 100% 缩放开发机的界面验证：XIANZHI_FORCE_SCALE=1 时锁定密度
        val forceScale = System.getenv("XIANZHI_FORCE_SCALE") == "1"
        CompositionLocalProvider(
            LocalDensity provides if (forceScale) Density(1f, 1f) else LocalDensity.current,
        ) {
            if (ready != null) {
                // 调试深链：XIANZHI_ARTICLE=first 时打开全书第一篇
                val firstCategory = runCatching {
                    ready.queries.categories().firstOrNull()
                }.getOrNull()
                val firstCat = firstCategory?.name
                val firstBook = firstCategory?.books?.firstOrNull()
                val effectiveArticle = startArticle ?: if (articleArg == "first") {
                    runCatching {
                        ready.queries.categories()
                            .firstNotNullOfOrNull { c ->
                                c.books.firstNotNullOfOrNull { b ->
                                    ready.queries.articlesOfBook(b.id).firstOrNull()?.id
                                }
                            }
                    }.getOrNull()
                } else {
                    null
                }
                XianzhiApp(
                    ready,
                    initialTab = startTab,
                    initialArticleId = effectiveArticle,
                    initialCat = catArg?.let { if (it == "first") firstCat else it },
                    initialBookId = bookArg?.let { if (it == "first") firstBook?.id else it },
                    initialBookTitle = if (bookArg != null) firstBook?.title.orEmpty() else "",
                )
            } else {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (error == null) {
                            Text("西安地方志", fontSize = 22.sp)
                            CircularProgressIndicator(Modifier.padding(top = 16.dp))
                            Text("正在准备数据…", fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                        } else {
                            Text("数据未就绪", fontSize = 20.sp)
                            Text(
                                error!!,
                                fontSize = 13.sp,
                                modifier = Modifier.width(480.dp).padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
