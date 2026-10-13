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
}
