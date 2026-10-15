package org.xadfz.xianzhi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.ui.screens.BooksTab
import org.xadfz.xianzhi.ui.screens.FavoritesScreen
import org.xadfz.xianzhi.ui.screens.HistoryScreen
import org.xadfz.xianzhi.ui.screens.ReaderScreen
import org.xadfz.xianzhi.ui.screens.SearchScreen
import org.xadfz.xianzhi.ui.screens.SettingsScreen

/**
 * 双端共享的 App 根组件。
 *
 * 导航结构：
 * - 三个一级功能（书目 / 检索 / 设置）以底栏（窄屏）或侧栏（宽屏）**全局常驻**，
 *   任何页面层级（含阅读正文页）都可见、可一键直达，无需呼出操作；
 * - 返回只在「上级页面层级」间发生，阅读内翻篇不改变返回路径（见 [AppNavigator]）。
 */
@Composable
fun XianzhiApp(
    container: AppContainer,
    initialTab: Int = 0,
    initialArticleId: Long? = null,
    // 以下仅用于桌面端调试深链，正式使用均为默认值
    initialCat: String? = null,
    initialBookId: String? = null,
    initialBookTitle: String = "",
    initialHighlight: String? = null,
    initialQuery: String = "",
) {
    val navigator = remember {
        AppNavigator().apply {
            if (initialTab in 1..3) goTab(initialTab)
            initialCat?.let { push(Route.BookList(it)) }
            if (initialBookId != null) push(Route.ArticleList(initialBookId, initialBookTitle))
            initialArticleId?.let { openChapter(it, initialHighlight) }
        }
    }
    val settings = remember { SettingsState(container.settings) }

    CompositionLocalProvider(LocalSettings provides settings) {
        XianzhiTheme(themeMode = settings.themeMode) {
            // 系统返回：仅当存在上级层级时拦截；一级页交给系统（Android 退出 App）
            PlatformBackHandler(enabled = navigator.stack.size > 1) { navigator.back() }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= RailBreakpoint) {
                    // 宽屏：常驻侧栏 + 内容
                    Row(Modifier.fillMaxSize()) {
                        MainNavRail(
                            selectedIndex = navigator.tab,
                            onSelect = { navigator.goTab(it) },
                            modifier = Modifier.fillMaxHeight(),
                        )
                        CurrentScreen(container, navigator, Modifier.fillMaxSize(), initialQuery)
                    }
                } else {
                    // 窄屏：常驻底栏 + 内容（阅读页也保留底栏）
                    Scaffold(
                        bottomBar = {
                            MainNavBar(
                                selectedIndex = navigator.tab,
                                onSelect = { navigator.goTab(it) },
                            )
                        },
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            CurrentScreen(container, navigator, Modifier.fillMaxSize(), initialQuery)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentScreen(
    container: AppContainer,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
) {
    when (navigator.current) {
        is Route.Tabs -> when (navigator.tab) {
            0 -> BooksTab(container, navigator, modifier)
            1 -> SearchScreen(container, navigator, modifier, initialQuery = initialQuery)
            2 -> FavoritesScreen(container, navigator, modifier)
            3 -> SettingsScreen(container, navigator, modifier)
            else -> BooksTab(container, navigator, modifier)
        }
        is Route.BookList, is Route.ArticleList -> BooksTab(container, navigator, modifier)
        is Route.History -> HistoryScreen(container, navigator, modifier)
        is Route.Reader -> {
            val route = navigator.current as Route.Reader
            ReaderScreen(
                container = container,
                articleId = route.articleId,
                highlight = route.highlight,
                onBack = { navigator.back() },
                // 上/下一篇、文内互见均为同级替换：返回始终回到来源层级
                onOpenArticle = { id -> navigator.openChapter(id) },
                modifier = modifier,
            )
        }
    }
}
