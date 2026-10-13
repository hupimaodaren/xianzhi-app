package org.xadfz.xianzhi.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchSnippetTest {

    @Test
    fun includesTwoLinesBeforeAndAfterHit() {
        val body = (1..10).joinToString("\n") { "第${it}行内容" }
        val snip = SearchSnippet.build(body, "第6行")!!
        assertEquals(listOf("第4行内容", "第5行内容", "第6行内容", "第7行内容", "第8行内容"), snip.lines)
        assertEquals(2, snip.hitLine)
        assertTrue(snip.truncatedBefore)
        assertTrue(snip.truncatedAfter)
    }

    @Test
    fun hitNearStartHasNoLeadingTruncation() {
        val body = (1..4).joinToString("\n") { "第${it}行" }
        val snip = SearchSnippet.build(body, "第1行")!!
        // 命中行 + 其后 2 行上下文；第 4 行在窗口之外
        assertEquals(listOf("第1行", "第2行", "第3行"), snip.lines)
        assertEquals(0, snip.hitLine)
        assertFalse(snip.truncatedBefore)
        assertTrue(snip.truncatedAfter)
    }

    @Test
    fun imageMarkersRemovedFromLines() {
        val snip = SearchSnippet.build("前文\n命中⟦img:abc.jpg⟧关键词\n后文", "关键词")!!
        assertEquals(listOf("前文", "命中关键词", "后文"), snip.lines)
        assertEquals(1, snip.hitLine)
    }

    @Test
    fun blankLinesIgnoredWhenCountingContext() {
        val body = "前一行\n\n  \n命中行\n\n后一行"
        val snip = SearchSnippet.build(body, "命中")!!
        assertEquals(listOf("前一行", "命中行", "后一行"), snip.lines)
        assertEquals(1, snip.hitLine)
    }

    @Test
    fun titleOnlyHitFallsBackToBodyHead() {
        val snip = SearchSnippet.build("首段\n次段\n三段", "正文里没有的词")!!
        assertFalse(snip.bodyHit)
        assertEquals(-1, snip.hitLine)
        assertEquals(listOf("首段", "次段", "三段"), snip.lines)
    }

    @Test
    fun shortHitLineKeptAsIs() {
        val line = "这是一行较短的内容，含关键词"
        val snip = SearchSnippet.build(line, "关键词")!!
        assertEquals(line, snip.lines.first())
    }

    @Test
    fun longHitLineWindowedAroundKeyword() {
        // 80 字符行，关键词「关键词」位于下标 60
        val line = (0 until 60).joinToString("") { "前" } + "关键词" + (0 until 18).joinToString("") { "后" }
        val snip = SearchSnippet.build(line, "关键词")!!
        val hit = snip.lines[snip.hitLine]
        assertTrue(line.length > SearchSnippet.LINE_WINDOW)
        // 关键词居中：前 20 字、关键词本身、后 18 字（到行尾），头部截断补省略号
        assertTrue(hit.startsWith("…"))
        assertFalse(hit.endsWith("…"))
        assertTrue(hit.contains("关键词"))
        assertEquals(SearchSnippet.WINDOW_SIDE + "关键词".length + 18 + 1, hit.length)
        assertTrue(line.contains(hit.removePrefix("…")))
    }

    @Test
    fun longHitLineKeywordNearBothEnds() {
        // 关键词在行首：无头部省略号，尾部截断补省略号
        val head = "关键词" + (0 until 60).joinToString("") { "字" }
        val hitHead = SearchSnippet.windowAroundKeyword(head, "关键词")
        assertFalse(hitHead.startsWith("…"))
        assertTrue(hitHead.endsWith("…"))
        assertTrue(hitHead.contains("关键词"))

        // 关键词在行尾：头部省略号，无尾部省略号
        val tail = (0 until 60).joinToString("") { "字" } + "关键词"
        val hitTail = SearchSnippet.windowAroundKeyword(tail, "关键词")
        assertTrue(hitTail.startsWith("…"))
        assertFalse(hitTail.endsWith("…"))
        assertTrue(hitTail.contains("关键词"))
    }

    @Test
    fun contextLinesNotWindowed() {
        // 命中行窗口化不影响上下文行：上下文行即便超长也原样保留
        val longCtx = (0 until 80).joinToString("") { "上" }
        val body = "$longCtx\n短命中行\n$longCtx"
        val snip = SearchSnippet.build(body, "短命中")!!
        assertEquals(1, snip.hitLine)
        assertEquals(longCtx, snip.lines[0])
        assertEquals(longCtx, snip.lines[2])
        assertEquals("短命中行", snip.lines[1])
    }
}
