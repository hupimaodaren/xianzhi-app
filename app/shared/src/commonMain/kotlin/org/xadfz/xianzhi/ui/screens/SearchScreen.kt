package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.db.Queries
import org.xadfz.xianzhi.model.SearchHit
import org.xadfz.xianzhi.search.SearchSnippet
import org.xadfz.xianzhi.ui.AppNavigator
import org.xadfz.xianzhi.ui.comfortableSidePadding
import org.xadfz.xianzhi.util.ioDispatcher

private val HIGHLIGHT_COLOR = Color(0xFFFFEB3B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    // 仅桌面端调试深链使用：启动即执行一次检索
    initialQuery: String = "",
) {
    val session = container.searchSession
    var searching by remember { mutableStateOf(false) }
    var historyVersion by remember { mutableIntStateOf(0) }
    val history = remember(historyVersion) { container.searchHistory.list() }
    val scope = rememberCoroutineScope()
    // 滚动位置从 session 恢复，保证从阅读页返回时仍在原结果位置
    val listState = rememberLazyListState(session.scrollIndex, session.scrollOffset)
    val totalArticles by produceState<Int?>(null) {
        value = withContext(ioDispatcher) { container.queries.totalArticles() }
    }

    fun doSearch(kw: String) {
        if (kw.isBlank()) return
        session.query = kw
        session.searched = true
        container.searchHistory.add(kw)
        historyVersion++
        scope.launch {
            searching = true
            val (hits, total) = withContext(ioDispatcher) {
                container.queries.countHits(kw) to container.queries.search(kw)
            }
            session.totalHits = hits
            session.results = total
            session.activeKeyword = kw
            session.resetForNewSearch()
            searching = false
        }
    }

    val pageCount = (session.results.size + Queries.SEARCH_PAGE_SIZE - 1) / Queries.SEARCH_PAGE_SIZE
    // 翻页时回到列表顶部；新搜索已在 resetForNewSearch 清零
    LaunchedEffect(session.page, session.activeKeyword) {
        if (session.results.isNotEmpty() && session.scrollIndex == 0 && session.scrollOffset == 0) {
            listState.scrollToItem(0)
        }
    }
    // 调试深链：带初始关键词时自动执行一次检索（仅当 session 尚无结果）
    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank() && session.results.isEmpty()) doSearch(initialQuery)
    }
    // 离开搜索页时记录滚动位置，供返回时恢复
    DisposableEffect(Unit) {
        onDispose {
            session.scrollIndex = listState.firstVisibleItemIndex
            session.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("全文检索") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = comfortableSidePadding(maxContent = 820.dp)
            Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = session.query,
                onValueChange = { session.query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = side, end = side, top = 8.dp, bottom = 4.dp),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text("全库检索 ${totalArticles ?: 38978} 篇志书文献") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (session.query.isNotEmpty()) {
                        androidx.compose.material3.IconButton(onClick = {
                            session.clear()
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { doSearch(session.query) },
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            when {
                searching -> Row(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "检索中（全表扫描约 1–3 秒）…",
                        Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                session.results.isNotEmpty() -> {
                    val capped = session.totalHits > Queries.SEARCH_MAX_HITS
                    val rangeFrom = session.page * Queries.SEARCH_PAGE_SIZE + 1
                    val rangeTo = minOf(
                        (session.page + 1) * Queries.SEARCH_PAGE_SIZE, session.results.size,
                    )
                    Text(
                        buildString {
                            append("命中 ${session.totalHits} 篇 · 当前显示第 $rangeFrom–$rangeTo 篇")
                            if (capped) append("（全文最多展示前 ${Queries.SEARCH_MAX_HITS} 篇）")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = side, end = side, top = 8.dp, bottom = 4.dp),
                    )
                    val pageItems = remember(session.results, session.page) {
                        session.results.subList(
                            session.page * Queries.SEARCH_PAGE_SIZE,
                            minOf((session.page + 1) * Queries.SEARCH_PAGE_SIZE, session.results.size),
                        )
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = side, end = side, top = 4.dp, bottom = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(pageItems, key = { it.id }) { hit ->
                            ResultCard(
                                hit = hit,
                                keyword = session.activeKeyword,
                                onClick = { navigator.openChapter(hit.id, session.activeKeyword) },
                            )
                        }
                        if (pageCount > 1) {
                            item(key = "pager") {
                                PagerBar(
                                    page = session.page,
                                    pageCount = pageCount,
                                    onPrev = { if (session.page > 0) session.page-- },
                                    onNext = { if (session.page < pageCount - 1) session.page++ },
                                )
                            }
                        }
                    }
                }

                history.isNotEmpty() -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = side, end = side - 8.dp, top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "搜索历史",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = {
                            container.searchHistory.clear()
                            historyVersion++
                        }) { Text("清除") }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = side, end = side, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(history) { kw ->
                            ResultCard(
                                hit = null,
                                keyword = kw,
                                fallbackTitle = kw,
                                fallbackSubtitle = "点击再次检索",
                                onClick = { doSearch(kw) },
                            )
                        }
                    }
                }

                else -> SearchEmpty(
                    hasQuery = session.query.isNotBlank(),
                    noResult = session.searched,
                    onSearch = { doSearch(session.query) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        }
    }
}

/** 底部分页条：每页 50 条，最多 10 页（500 条上限） */
@Composable
private fun PagerBar(page: Int, pageCount: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrev, enabled = page > 0) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text("上一页")
        }
        Text(
            "第 ${page + 1} / $pageCount 页",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        TextButton(onClick = onNext, enabled = page < pageCount - 1) {
            Text("下一页")
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ResultCard(
    hit: SearchHit?,
    keyword: String,
    fallbackTitle: String = "",
    fallbackSubtitle: String = "",
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (hit != null) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    highlightText(hit?.title ?: fallbackTitle, keyword),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    hit?.let { "${it.cat} · ${it.bookTitle}" } ?: fallbackSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                val snip = hit?.snippet
                if (snip != null) {
                    Spacer(Modifier.height(6.dp))
                    SnippetView(snip, keyword)
                }
            }
        }
    }
}

/** 命中原文片段：命中行高亮显示，前后各 2 行为上下文；关键词黄底标记 */
@Composable
private fun SnippetView(snippet: SearchSnippet, keyword: String) {
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column {
        snippet.lines.forEachIndexed { i, line ->
            val isHit = i == snippet.hitLine
            if (i == 0 && snippet.truncatedBefore) {
                Text("…", style = MaterialTheme.typography.bodySmall, color = onVariant)
            }
            Text(
                highlightText(line, keyword),
                style = if (isHit) MaterialTheme.typography.bodyMedium
                else MaterialTheme.typography.bodySmall,
                fontWeight = if (isHit) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isHit) onSurface else onVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (i == snippet.lines.lastIndex && snippet.truncatedAfter) {
                Text("…", style = MaterialTheme.typography.bodySmall, color = onVariant)
            }
        }
        if (!snippet.bodyHit) {
            Text(
                "（标题命中，正文无该关键词）",
                style = MaterialTheme.typography.labelSmall,
                color = onVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** 给文本中的关键词加黄底，返回带注解字符串 */
private fun highlightText(text: String, keyword: String): AnnotatedString {
    if (keyword.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val idx = text.indexOf(keyword, start)
            if (idx < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, idx))
            withStyle(SpanStyle(background = HIGHLIGHT_COLOR)) {
                append(text.substring(idx, idx + keyword.length))
            }
            start = idx + keyword.length
        }
    }
}

@Composable
private fun SearchEmpty(
    hasQuery: Boolean,
    noResult: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    noResult -> "未找到相关篇目，换个关键词试试"
                    hasQuery -> "输入完成后回车或点此开始检索"
                    else -> "输入关键词，全库检索志书文献"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(enabled = hasQuery && !noResult) { onSearch() },
            )
            Spacer(Modifier.height(6.dp))
            if (!noResult) {
                Text(
                    "可试试：西安 · 城墙 · 县志 · 人物",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
