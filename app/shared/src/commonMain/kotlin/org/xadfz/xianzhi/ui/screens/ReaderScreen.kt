package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.model.ArticleDetail
import org.xadfz.xianzhi.model.Neighbor
import org.xadfz.xianzhi.settings.SettingsKeys
import org.xadfz.xianzhi.ui.LocalSettings
import org.xadfz.xianzhi.ui.RichBody
import org.xadfz.xianzhi.util.ioDispatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    container: AppContainer,
    articleId: Long,
    highlight: String?,
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current!!

    data class ReaderData(
        val detail: ArticleDetail,
        val prev: Neighbor?,
        val next: Neighbor?,
    )

    val data by produceState<ReaderData?>(null, articleId) {
        value = withContext(ioDispatcher) {
            val d = container.queries.articleDetail(articleId) ?: return@withContext null
            ReaderData(
                detail = d,
                prev = container.queries.neighbor(d.bookId, articleId, next = false),
                next = container.queries.neighbor(d.bookId, articleId, next = true),
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        data?.detail?.bookTitle ?: "阅读",
                        maxLines = 1,
                    )
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
        bottomBar = {
            if (data != null) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth()) {
                    val prev = data!!.prev
                    val next = data!!.next
                    ReaderNavButton(
                        enabled = prev != null,
                        icon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) },
                        label = prev?.title ?: "已是第一篇",
                        onClick = { prev?.let { onOpenArticle(it.id) } },
                        modifier = Modifier.weight(1f),
                    )
                    ReaderNavButton(
                        enabled = next != null,
                        icon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        label = next?.title ?: "已是最后一篇",
                        onClick = { next?.let { onOpenArticle(it.id) } },
                        modifier = Modifier.weight(1f),
                        end = true,
                    )
                }
            }
        },
    ) { padding ->
        val d = data
        if (d == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val listState = remember(articleId) {
            androidx.compose.foundation.lazy.LazyListState(0, 0)
        }
        // 恢复阅读位置
        LaunchedEffect(articleId) {
            val idx = container.settings.getInt(SettingsKeys.readerIndex(articleId), 0)
            val off = container.settings.getInt(SettingsKeys.readerOffset(articleId), 0)
            if (idx > 0 || off > 0) listState.scrollToItem(idx, off)
        }
        // 离开页面时记忆位置
        DisposableEffect(articleId) {
            onDispose {
                container.settings.putInt(
                    SettingsKeys.readerIndex(articleId), listState.firstVisibleItemIndex,
                )
                container.settings.putInt(
                    SettingsKeys.readerOffset(articleId), listState.firstVisibleItemScrollOffset,
                )
            }
        }

        // 桌面键盘：左右方向键翻篇（Windows 增强）
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyUp) {
                        when (e.key) {
                            Key.DirectionLeft -> {
                                d.prev?.let { onOpenArticle(it.id) }; true
                            }
                            Key.DirectionRight -> {
                                d.next?.let { onOpenArticle(it.id) }; true
                            }
                            else -> false
                        }
                    } else false
                },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp,
            ),
        ) {
            item(key = "header-${d.detail.id}") {
                Text(
                    d.detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                val meta = listOfNotNull(
                    d.detail.vol.ifBlank { null },
                    d.detail.date.ifBlank { null },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                    )
                }
            }
            item(key = "body-${d.detail.id}") {
                RichBody(
                    body = d.detail.body,
                    container = container,
                    fontSize = settings.fontSize,
                    lineSpacing = settings.lineSpacing,
                    keyword = highlight,
                )
            }
        }
    }
}

@Composable
private fun ReaderNavButton(
    enabled: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    end: Boolean = false,
) {
    Row(
        modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (end) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            if (!end) icon()
        }
        Text(
            label,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onClick, enabled = enabled) {
            if (end) icon()
        }
    }
}
