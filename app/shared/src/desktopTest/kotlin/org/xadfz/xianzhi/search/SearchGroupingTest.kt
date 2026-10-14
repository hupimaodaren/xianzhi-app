package org.xadfz.xianzhi.search

import org.xadfz.xianzhi.db.DbSchema
import org.xadfz.xianzhi.model.SearchHit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchGroupingTest {

    private fun hit(id: Long, cat: String) =
        SearchHit(id = id, title = "篇目$id", book = "b$id", bookTitle = "书名$id",
            cat = cat, snippet = null)

    @Test
    fun emptyInputYieldsNoGroups() {
        assertTrue(SearchGrouping.groupByCategory(emptyList()).isEmpty())
    }

    @Test
    fun everyHitAssignedExactlyOnce() {
        val hits = listOf(
            hit(1, DbSchema.CAT_XIAN),
            hit(2, DbSchema.CAT_DISTRICT),
            hit(3, DbSchema.CAT_XIAN),
            hit(4, DbSchema.CAT_YEARBOOK),
        )
        val groups = SearchGrouping.groupByCategory(hits)
        // 无遗漏：分组内条数合计等于总结果数，且 id 集合完全一致
        assertEquals(hits.size, groups.sumOf { it.hits.size })
        assertEquals(hits.map { it.id }, groups.flatMap { it.hits }.map { it.id }.sorted())
        // 无错配：每组内所有条目的分类与组名一致
        groups.forEach { g -> assertTrue(g.hits.all { it.cat == g.category }) }
    }

    @Test
    fun groupsFollowCategoryOrderRegardlessOfInputOrder() {
        // 输入顺序打乱，输出仍按 DbSchema.CATEGORY_ORDER 排列
        val hits = listOf(
            hit(1, DbSchema.CAT_YEARBOOK),
            hit(2, DbSchema.CAT_DISTRICT),
            hit(3, DbSchema.CAT_XIAN),
        )
        val cats = SearchGrouping.groupByCategory(hits).map { it.category }
        assertEquals(
            listOf(DbSchema.CAT_XIAN, DbSchema.CAT_DISTRICT, DbSchema.CAT_YEARBOOK),
            cats,
        )
    }

    @Test
    fun withinGroupKeepsOriginalSearchOrder() {
        // 检索原有排序：标题命中优先、再按 id。分组后组内顺序必须与之保持一致
        val hits = listOf(
            hit(7, DbSchema.CAT_XIAN),   // 标题命中，排最前
            hit(1, DbSchema.CAT_XIAN),
            hit(3, DbSchema.CAT_XIAN),
            hit(2, DbSchema.CAT_DISTRICT),
        )
        val groups = SearchGrouping.groupByCategory(hits)
        val xian = groups.first { it.category == DbSchema.CAT_XIAN }
        assertEquals(listOf(7L, 1L, 3L), xian.hits.map { it.id })
        val district = groups.first { it.category == DbSchema.CAT_DISTRICT }
        assertEquals(listOf(2L), district.hits.map { it.id })
    }

    @Test
    fun categoriesWithoutHitsAreOmitted() {
        val hits = listOf(hit(1, DbSchema.CAT_OLD))
        val groups = SearchGrouping.groupByCategory(hits)
        assertEquals(1, groups.size)
        assertEquals(DbSchema.CAT_OLD, groups.single().category)
    }

    @Test
    fun unknownCategoryAppendedAfterKnownOnes() {
        val hits = listOf(
            hit(1, "未收录分类"),
            hit(2, DbSchema.CAT_XIAN),
        )
        val cats = SearchGrouping.groupByCategory(hits).map { it.category }
        assertEquals(listOf(DbSchema.CAT_XIAN, "未收录分类"), cats)
    }

    @Test
    fun customOrderIsRespected() {
        val hits = listOf(hit(1, "乙"), hit(2, "甲"))
        val cats = SearchGrouping.groupByCategory(hits, order = listOf("甲", "乙")).map { it.category }
        assertEquals(listOf("甲", "乙"), cats)
    }
}
