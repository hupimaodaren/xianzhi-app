package org.xadfz.xianzhi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/** 页面路由 */
sealed class Route {
    data object Tabs : Route()
    data class BookList(val cat: String) : Route()
    data class ArticleList(val bookId: String, val bookTitle: String) : Route()
    data class Reader(val articleId: Long, val highlight: String? = null) : Route()
}

/** 极简导航：底部三 Tab + 书目栈；检索结果直接压入 Reader */
class AppNavigator {
    var tab by mutableIntStateOf(0)
        private set
    val stack = mutableStateListOf<Route>(Route.Tabs)

    val current: Route get() = stack.last()

    fun push(route: Route) {
        stack.add(route)
    }

    /** 返回上一级；已在根页面返回 false */
    fun back(): Boolean {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        return false
    }

    fun goTab(index: Int) {
        tab = index
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}
