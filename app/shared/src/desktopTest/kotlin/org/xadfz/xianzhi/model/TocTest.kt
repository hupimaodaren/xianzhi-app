package org.xadfz.xianzhi.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TocTest {

    private fun art(id: Long, title: String, vararg path: String) =
        Article(id, title, path.firstOrNull().orEmpty(), "", path.joinToString("/"))

    @Test
    fun groupsByPathPrefixNotByFixedLevel() {
        // 同一父节点下子层级深度不齐：直接挂 1 级与 3 级节点
        val toc = buildToc(listOf(
            art(1, "甲文", "甲"),
            art(2, "乙文", "乙", "乙一", "乙一一"),
        ))
        assertEquals(2, toc.size)
        val (g1, g2) = toc
        assertIs<TocGroup>(g1)
        assertIs<TocGroup>(g2)
        assertEquals("甲", g1.title)
        assertEquals("乙", g2.title)
        // 甲 下直接是叶子（1 级）
        assertEquals(1, (g1 as TocGroup).children.size)
        assertIs<TocLeaf>(g1.children.first())
        // 乙 下是两层中间分组（path 全部是祖先段），叶子挂在最深分组下
        assertEquals(1, (g2 as TocGroup).children.size)
        val mid = g2.children.first()
        assertIs<TocGroup>(mid)
        assertEquals("乙一", mid.title)
        assertEquals(1, (mid as TocGroup).children.size)
        val deep = mid.children.first()
        assertIs<TocGroup>(deep)
        assertEquals("乙一一", deep.title)
        assertEquals(1, (deep as TocGroup).children.size)
        val leaf = deep.children.first()
        assertIs<TocLeaf>(leaf)
        assertEquals("乙文", leaf.title)
    }

    @Test
    fun rootArticlesListedAtRoot() {
        val toc = buildToc(listOf(art(1, "凡例"), art(2, "编纂委员会"), art(3, "正文", "志一")))
        assertEquals(3, toc.size)
        assertIs<TocLeaf>(toc[0])
        assertIs<TocLeaf>(toc[1])
        assertIs<TocGroup>(toc[2])
        assertEquals("凡例", toc[0].title)
    }

    @Test
    fun leafCountCountsDescendants() {
        val toc = buildToc(listOf(
            art(1, "a", "志"),
            art(2, "b", "志", "目"),
            art(3, "c", "志", "目", "节"),
        ))
        val g = toc.filterIsInstance<TocGroup>().single()
        assertEquals(3, g.leafCount)
    }

    @Test
    fun nodeThatIsBothGroupAndArticle() {
        // 数据源中存在一篇其路径正好等于某分组名（全库仅一例）
        val toc = buildToc(listOf(
            art(1, "财政", "财政"),
            art(2, "税收", "财政", "税收"),
        ))
        val g = toc.filterIsInstance<TocGroup>().single()
        assertEquals(1L, g.articleId)
        assertEquals(2, g.leafCount)
    }

    @Test
    fun orderFollowsArticleOrder() {
        val toc = buildToc(listOf(
            art(1, "a", "乙"),
            art(2, "b", "甲"),
            art(3, "c", "甲"),
        ))
        assertEquals(listOf("乙", "甲"), toc.map { it.title })
    }

    @Test
    fun defaultExpandsOnlyRootGroups() {
        val toc = buildToc(listOf(
            art(1, "a", "甲", "甲一", "甲一一"),
            art(2, "b", "乙"),
        ))
        val expanded = defaultExpanded(toc)
        assertEquals(setOf("甲", "乙"), expanded)
        val flat = flattenToc(toc, expanded)
        // 两级分组 + 各自叶子；第三级不展开
        assertEquals(4, flat.size)
        assertTrue(flat.none { it.key == "甲/甲一/甲一一" })
    }

    @Test
    fun flattenRespectsExpandedState() {
        val toc = buildToc(listOf(art(1, "a", "甲", "甲一")))
        val collapsed = flattenToc(toc, emptySet())
        assertEquals(1, collapsed.size)
        val expanded = flattenToc(toc, setOf("甲"))
        assertEquals(2, expanded.size)
        assertEquals(listOf("甲", "甲一"), expanded.map { it.title })
    }

    @Test
    fun duplicateTitlesUnderSameParentGetDistinctKeys() {
        val toc = buildToc(listOf(art(1, "同名", "甲"), art(2, "同名", "甲")))
        val g = toc.filterIsInstance<TocGroup>().single()
        val flat = flattenToc(toc, setOf("甲"))
        assertEquals(3, flat.size)
        assertEquals(2, (g as TocGroup).children.size)
        assertEquals(flat.map { it.key }.distinct().size, flat.size)
    }
}
