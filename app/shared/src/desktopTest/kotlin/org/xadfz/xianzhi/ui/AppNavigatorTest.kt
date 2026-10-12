package org.xadfz.xianzhi.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigatorTest {

    private fun navigatorWithReader(): AppNavigator {
        val n = AppNavigator()
        n.push(Route.BookList("综合志"))
        n.push(Route.ArticleList("b1", "西安市志"))
        n.openChapter(100L)
        return n
    }

    @Test
    fun chapterSwitchReplacesStackTop() {
        val n = navigatorWithReader()
        // 连续「下一篇」5 次 + 文内互见 1 次
        n.openChapter(101L)
        n.openChapter(102L)
        n.openChapter(103L)
        n.openChapter(104L)
        n.openChapter(105L)
        n.openChapter(999L, "互见词")

        assertEquals(4, n.stack.size, "同级切篇不得增加导航栈层级")
        assertEquals(999L, (n.current as Route.Reader).articleId)
        assertEquals("互见词", (n.current as Route.Reader).highlight)
    }

    @Test
    fun backAfterChapterBrowsingReturnsToArticleList() {
        val n = navigatorWithReader()
        n.openChapter(101L)
        n.openChapter(102L)

        assertTrue(n.back(), "阅读页返回应消费返回事件")
        assertTrue(n.current is Route.ArticleList, "返回应直接回篇目列表，而非上一篇章节")

        assertTrue(n.back())
        assertTrue(n.current is Route.BookList, "再次返回回到书目列表")
        assertTrue(n.back())
        assertEquals(Route.Tabs, n.current, "再次返回回到一级首页")
        assertFalse(n.back(), "一级页面返回交由系统处理")
    }

    @Test
    fun backFromReaderOpenedViaSearchReturnsToSearchTab() {
        val n = AppNavigator()
        n.goTab(1) // 检索 Tab
        n.openChapter(555L, "西安")

        assertEquals(2, n.stack.size)
        assertTrue(n.back())
        assertEquals(Route.Tabs, n.current, "检索结果打开正文，返回回到检索页（Tab 根）")
        assertEquals(1, n.tab)
    }

    @Test
    fun goTabResetsHierarchyStack() {
        val n = navigatorWithReader()
        assertEquals(4, n.stack.size)
        n.goTab(2) // 设置
        assertEquals(1, n.stack.size)
        assertEquals(Route.Tabs, n.current)
        assertEquals(2, n.tab)
    }

    @Test
    fun openChapterOnRootPushesReader() {
        val n = AppNavigator()
        n.openChapter(1L)
        assertEquals(2, n.stack.size)
        assertTrue(n.current is Route.Reader)
    }
}
