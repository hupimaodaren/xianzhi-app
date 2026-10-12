package org.xadfz.xianzhi.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.ui.screens.BooksTab
import org.xadfz.xianzhi.ui.screens.ReaderScreen
import org.xadfz.xianzhi.ui.screens.SearchScreen
import org.xadfz.xianzhi.ui.screens.SettingsScreen

/**
 * 双端共享的 App 根组件：底部三栏导航（书目/检索/设置）+ 书目栈 + 阅读页。
 */
@Composable
fun XianzhiApp(container: AppContainer) {
    val navigator = remember { AppNavigator() }
    val settings = remember { SettingsState(container.settings) }

    CompositionLocalProvider(LocalSettings provides settings) {
        XianzhiTheme(themeMode = settings.themeMode) {
            PlatformBackHandler(enabled = navigator.stack.size > 1) { navigator.back() }

            Scaffold(
                bottomBar = {
                    if (navigator.current is Route.Tabs) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = navigator.tab == 0,
                                onClick = { navigator.goTab(0) },
                                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                label = { Text("书目") },
                            )
                            NavigationBarItem(
                                selected = navigator.tab == 1,
                                onClick = { navigator.goTab(1) },
                                icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                label = { Text("检索") },
                            )
                            NavigationBarItem(
                                selected = navigator.tab == 2,
                                onClick = { navigator.goTab(2) },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                label = { Text("设置") },
                            )
                        }
                    }
                },
            ) { padding ->
                when (val route = navigator.current) {
                    is Route.Tabs -> when (navigator.tab) {
                        0 -> BooksTab(container, navigator, Modifier.padding(padding))
                        1 -> SearchScreen(container, navigator, Modifier.padding(padding))
                        2 -> SettingsScreen(container, Modifier.padding(padding))
                    }
                    is Route.Reader -> ReaderScreen(
                        container = container,
                        articleId = route.articleId,
                        highlight = route.highlight,
                        onBack = { navigator.back() },
                        onOpenArticle = { id -> navigator.push(Route.Reader(id)) },
                        modifier = Modifier.padding(padding),
                    )
                    else -> BooksTab(container, navigator, Modifier.padding(padding))
                }
            }
        }
    }
}
