package org.xadfz.xianzhi.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.xadfz.xianzhi.model.SearchHit

/**
 * 搜索会话状态：由 [AppContainer] 持有，跨页面（搜索结果→阅读→返回）保留。
 *
 * SearchScreen 离开组合树时其 `remember` 状态会丢失，故将检索结果、当前页、
 * 滚动位置等提升到此处，返回搜索页时即可恢复到离开前的状态。
 */
class SearchSession {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<SearchHit>>(emptyList())
    var totalHits by mutableIntStateOf(0)
    var activeKeyword by mutableStateOf("")
    var searched by mutableStateOf(false)

    /** 当前展开的书目大分类集合；默认全部收起，仅显示分类名与命中数 */
    var expandedCats by mutableStateOf<Set<String>>(emptySet())

    /** 离开搜索页时记录的列表滚动位置，返回时恢复 */
    var scrollIndex by mutableIntStateOf(0)
    var scrollOffset by mutableIntStateOf(0)

    /** 翻页/新搜索后请求回到列表顶部（从阅读页返回时不置位，以恢复原位置） */
    var requestScrollTop by mutableStateOf(false)

    /** 结果被截断的上限；0 表示完整展示 */
    var cappedAt by mutableIntStateOf(0)

    /** 执行新搜索或清除时调用，重置展开状态与滚动 */
    fun resetForNewSearch() {
        expandedCats = emptySet()
        scrollIndex = 0
        scrollOffset = 0
        requestScrollTop = true
        cappedAt = 0
    }

    /** 展开/收起某个书目大分类 */
    fun toggleCategory(cat: String) {
        expandedCats = if (cat in expandedCats) expandedCats - cat else expandedCats + cat
    }

    fun clear() {
        query = ""
        results = emptyList()
        totalHits = 0
        activeKeyword = ""
        expandedCats = emptySet()
        searched = false
        scrollIndex = 0
        scrollOffset = 0
        requestScrollTop = false
        cappedAt = 0
    }
}
