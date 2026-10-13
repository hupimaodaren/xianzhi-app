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
    var page by mutableIntStateOf(0)
    var searched by mutableStateOf(false)

    /** 离开搜索页时记录的列表滚动位置，返回时恢复 */
    var scrollIndex by mutableIntStateOf(0)
    var scrollOffset by mutableIntStateOf(0)

    /** 执行新搜索或清除时调用，重置分页与滚动 */
    fun resetForNewSearch() {
        page = 0
        scrollIndex = 0
        scrollOffset = 0
    }

    fun clear() {
        query = ""
        results = emptyList()
        totalHits = 0
        activeKeyword = ""
        page = 0
        searched = false
        scrollIndex = 0
        scrollOffset = 0
    }
}
