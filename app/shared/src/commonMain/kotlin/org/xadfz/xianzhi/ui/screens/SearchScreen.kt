package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.model.SearchHit
import org.xadfz.xianzhi.ui.AppNavigator
import org.xadfz.xianzhi.ui.Route
import org.xadfz.xianzhi.util.ioDispatcher

@Composable
fun SearchScreen(
    container: AppContainer,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var historyVersion by remember { mutableStateOf(0) }
    val history = remember(historyVersion) { container.searchHistory.list() }
    val scope = rememberCoroutineScope()

    fun doSearch(kw: String) {
        if (kw.isBlank()) return
        query = kw
        container.searchHistory.add(kw)
        historyVersion++
        scope.launch {
            searching = true
            results = withContext(ioDispatcher) { container.queries.search(kw) }
            searching = false
        }
    }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true,
            placeholder = { Text("全库检索 38978 篇") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList() }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清除")
                    }
                }
            },
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { doSearch(query) },
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
        )

        if (searching) {
            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("检索中（全表扫描约 1–3 秒）…", Modifier.padding(start = 12.dp))
            }
        } else if (results.isEmpty()) {
            // 搜索历史（本地保存最近 20 条）
            if (history.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("搜索历史")
                    TextButton(onClick = {
                        container.searchHistory.clear()
                        historyVersion++
                    }) { Text("清除") }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(history) { kw ->
                        ListItem(
                            headlineContent = { Text(kw) },
                            modifier = Modifier.clickable { doSearch(kw) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        "命中 ${results.size} 篇（最多显示 200 篇）",
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(results, key = { it.id }) { hit ->
                    ListItem(
                        headlineContent = {
                            Text(hit.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text("${hit.cat} · ${hit.bookTitle}", maxLines = 1)
                        },
                        modifier = Modifier.clickable {
                            navigator.push(Route.Reader(hit.id, query))
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
