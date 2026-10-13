package org.xadfz.xianzhi.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TocDeepTest {

    private fun art(id: Long, title: String, vararg path: String): Article =
        Article(id, title, path.firstOrNull().orEmpty(), "", path.joinToString("/"))

    @Test
    fun arbitraryDepthSupported() {
        // 8 级深链，验证不预设层级上限
        val depth = 8
        val path = (1..depth).map { "第${it}级" }.toTypedArray()
        val toc = buildToc(listOf(art(1, "深层篇目", *path)))
        var node: TocNode = toc.single()
        repeat(depth - 1) {
            val g = node
            assertIs<TocGroup>(g)
            assertEquals(it + 1, g.level)
            node = g.children.single()
        }
        val leafGroup = node
        assertIs<TocGroup>(leafGroup)
        assertEquals(depth, leafGroup.level)
        val leaf = leafGroup.children.single()
        assertIs<TocLeaf>(leaf)
        assertEquals(depth + 1, leaf.level)
        // 全展开 key 数 = 8
        assertEquals(depth, allGroupKeys(toc).size)
        // 全展开后拍平：8 个分组 + 1 个叶子
        assertEquals(9, flattenToc(toc, allGroupKeys(toc)).size)
    }
}
