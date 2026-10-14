package org.xadfz.xianzhi.search

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchSessionTest {

    private lateinit var session: SearchSession

    @BeforeTest
    fun setup() {
        session = SearchSession()
    }

    @Test
    fun categoriesStartCollapsed() {
        assertTrue(session.expandedCats.isEmpty())
    }

    @Test
    fun toggleExpandsThenCollapses() {
        session.toggleCategory("西安市志")
        assertTrue("西安市志" in session.expandedCats)
        // 展开另一分类不影响已展开的分类（可多类同时展开）
        session.toggleCategory("区县志鉴")
        assertEquals(setOf("西安市志", "区县志鉴"), session.expandedCats)
        session.toggleCategory("西安市志")
        assertEquals(setOf("区县志鉴"), session.expandedCats)
    }

    @Test
    fun newSearchCollapsesAllAndRequestsScrollTop() {
        session.toggleCategory("西安市志")
        session.scrollIndex = 42
        session.resetForNewSearch()
        assertTrue(session.expandedCats.isEmpty())
        assertEquals(0, session.scrollIndex)
        assertTrue(session.requestScrollTop)
    }

    @Test
    fun clearResetsExpansion() {
        session.toggleCategory("西安市志")
        session.clear()
        assertTrue(session.expandedCats.isEmpty())
        assertFalse(session.requestScrollTop)
    }
}
