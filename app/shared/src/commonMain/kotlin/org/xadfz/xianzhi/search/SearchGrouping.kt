package org.xadfz.xianzhi.search

import org.xadfz.xianzhi.db.DbSchema
import org.xadfz.xianzhi.model.SearchHit

/** 检索结果按书目大分类聚合后的一组，[hits] 保持检索原有的排序 */
data class SearchGroup(
    val category: String,
    val hits: List<SearchHit>,
)

/**
 * 检索结果的书目大分类分组规则（纯函数，便于单测）。
 *
 * - 每条命中恰好归属一个分组，无遗漏、无错配；
 * - 分组内保持传入顺序，即检索原有排序（标题命中优先，再按 id）；
 * - 分组顺序按 [order]（默认 [DbSchema.CATEGORY_ORDER]），
 *   未在 order 中出现的分类按首次出现顺序追加到末尾；
 * - 没有任何命中的分类不产生分组。
 */
object SearchGrouping {

    fun groupByCategory(
        hits: List<SearchHit>,
        order: List<String> = DbSchema.CATEGORY_ORDER,
    ): List<SearchGroup> {
        if (hits.isEmpty()) return emptyList()
        val byCat = LinkedHashMap<String, MutableList<SearchHit>>()
        for (h in hits) byCat.getOrPut(h.cat) { mutableListOf() }.add(h)
        val ordered = order.filter { byCat.containsKey(it) } +
            byCat.keys.filter { it !in order }
        return ordered.map { SearchGroup(it, byCat.getValue(it)) }
    }
}
