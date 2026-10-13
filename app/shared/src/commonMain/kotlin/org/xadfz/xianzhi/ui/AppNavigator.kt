package org.xadfz.xianzhi.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/** 页面路由 */
sealed class Route {
    /** 三个一级 Tab 容器（具体显示哪个 Tab 由 [AppNavigator.tab] 决定） */
    data object Tabs : Route()
    data class BookList(val cat: String) : Route()
    data class ArticleList(val bookId: String, val bookTitle: String) : Route()
    data class Reader(val articleId: Long, val highlight: String? = null) : Route()
}

/**
 * 导航栈：一级 Tab 常驻栈底，书目层级（分类→书目→篇目→阅读）逐级压栈。
 *
 * 返回语义：[back] 永远只回退到「上级页面层级」。
 * 阅读页内的上一篇/下一篇/文内互见属于同级切换，用 [openChapter] 替换栈顶，
 * 不会产生「返回键回到上一篇阅读章节」的问题。
 */
class AppNavigator {
    var tab by mutableIntStateOf(0)
        private set
    val stack = mutableStateListOf<Route>(Route.Tabs)

    val current: Route get() = stack.last()

    /** 进入下一层级页面 */
    fun push(route: Route) {
        stack.add(route)
    }

    /**
     * 打开一篇正文：
     * - 从篇目列表/检索结果进入 → 压栈（返回回到来源页）；
     * - 已在阅读页（上一篇/下一篇/文内跳转）→ 同级替换，不改变返回层级。
     */
    fun openChapter(articleId: Long, highlight: String? = null) {
        val next = Route.Reader(articleId, highlight)
        if (stack.lastOrNull() is Route.Reader) {
            stack[stack.lastIndex] = next
        } else {
            stack.add(next)
        }
    }

    /** 返回上一级；已在一级页面返回 false（交由系统处理，如退出 App） */
    fun back(): Boolean {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        return false
    }

    /** 切换一级 Tab：书目层级栈整体收起到根（再次点当前 Tab 即回到该栏首页） */
    fun goTab(index: Int) {
        tab = index
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    // 各层级目录列表的滚动位置缓存：导航离开再返回时恢复到点击前的位置。
    // LazyListState 实例由导航器持有，不受目标页 composition 生命周期影响。
    private val listStates = mutableMapOf<String, LazyListState>()

    /** 取（或创建）某层级列表的滚动状态；同一 key 始终返回同一实例 */
    fun listState(key: String): LazyListState =
        listStates.getOrPut(key) { LazyListState() }
}
