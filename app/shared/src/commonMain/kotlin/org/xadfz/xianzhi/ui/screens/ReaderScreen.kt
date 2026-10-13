package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.model.ArticleDetail
import org.xadfz.xianzhi.model.Collection
import org.xadfz.xianzhi.model.Neighbor
import org.xadfz.xianzhi.settings.SettingsKeys
import org.xadfz.xianzhi.ui.LocalSettings
import org.xadfz.xianzhi.ui.ParagraphBlock
import org.xadfz.xianzhi.ui.comfortableSidePadding
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

    // 收藏状态：随文章切换重新查询；点击按钮即时更新本地态避免等待
    var favorited by remember(articleId) { mutableStateOf(false) }
    var currentCollId by remember(articleId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(articleId, container.favorites.version) {
        val fav = withContext(ioDispatcher) { container.favorites.getFavoriteByArticleId(articleId) }
        favorited = fav != null
        currentCollId = fav?.collectionId
    }

    // 合集列表：随版本变化自动刷新（含新建合集后即时刷新）
    val collections by produceState<List<Collection>>(emptyList(), container.favorites.version) {
        value = withContext(ioDispatcher) { container.favorites.listCollections() }
    }
    var showCollDialog by remember(articleId) { mutableStateOf(false) }

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
                actions = {
                    val d = data?.detail
                    if (d != null) {
                        FavoriteButton(
                            favorited = favorited,
                            onClick = { showCollDialog = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            data?.let { d ->
                ChapterNavBar(
                    prevTitle = d.prev?.title,
                    nextTitle = d.next?.title,
                    onPrev = { d.prev?.let { onOpenArticle(it.id) } },
                    onNext = { d.next?.let { onOpenArticle(it.id) } },
                )
            }
        },
    ) { padding ->
        val d = data
        if (d == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val paragraphs = remember(d.detail.body) {
            org.xadfz.xianzhi.render.splitParagraphs(d.detail.body)
        }
        // 检索跳入：首个包含关键词的段落（列表第 0 项是标题头，段落从第 1 项起）
        val hitParagraph = remember(d.detail.id, highlight) {
            val kw = highlight
            if (kw.isNullOrBlank()) -1
            else paragraphs.indexOfFirst {
                org.xadfz.xianzhi.render.plainText(it).contains(kw)
            }
        }

        val listState: LazyListState = remember(articleId) { LazyListState(0, 0) }
        // 定位：检索跳入时滚动到命中段落并居中；否则恢复上次阅读位置
        LaunchedEffect(articleId, highlight) {
            if (hitParagraph >= 0) {
                val itemIndex = hitParagraph + 1
                listState.scrollToItem(itemIndex)
                val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
                if (info != null) {
                    val viewportHeight =
                        listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                    val delta = info.offset + info.size / 2f -
                        (listState.layoutInfo.viewportStartOffset + viewportHeight / 2f)
                    listState.scroll { scrollBy(delta) }
                }
            } else {
                val idx = container.settings.getInt(SettingsKeys.readerIndex(articleId), 0)
                val off = container.settings.getInt(SettingsKeys.readerOffset(articleId), 0)
                if (idx > 0 || off > 0) listState.scrollToItem(idx, off)
            }
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

        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val hp = comfortableSidePadding(maxContent = 760.dp, min = 20.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
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
                        } else {
                            false
                        }
                    },
                contentPadding = PaddingValues(
                    start = hp, end = hp, top = 10.dp, bottom = 36.dp,
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
                ).joinToString("  ·  ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }
                // 标题下朱砂短分隔，典籍页眉感
                val accent = MaterialTheme.colorScheme.primary
                Box(
                    Modifier
                        .padding(bottom = 16.dp)
                        .size(width = 42.dp, height = 3.dp)
                        .drawBehind {
                            drawRect(color = accent)
                        },
                )
            }
            itemsIndexed(paragraphs, key = { i, _ -> "p-${d.detail.id}-$i" }) { _, paragraph ->
                ParagraphBlock(
                    paragraph = paragraph,
                    container = container,
                    fontSize = settings.fontSize,
                    lineSpacing = settings.lineSpacing,
                    keyword = highlight,
                )
            }
            }
        }
    }

    // 收藏 / 移动合集对话框
    val detailForDialog = data?.detail
    if (showCollDialog && detailForDialog != null) {
        AddToCollectionDialog(
            articleTitle = detailForDialog.title,
            collections = collections,
            currentCollectionId = currentCollId,
            favorited = favorited,
            onDismiss = { showCollDialog = false },
            onSelect = { collId ->
                if (favorited) {
                    container.favorites.moveToCollection(articleId, collId)
                } else {
                    container.favorites.addFavorite(
                        articleId = articleId,
                        title = detailForDialog.title,
                        bookId = detailForDialog.bookId,
                        bookTitle = detailForDialog.bookTitle,
                        cat = "",
                        collectionId = collId,
                    )
                    favorited = true
                }
                currentCollId = collId
                showCollDialog = false
            },
            onCreateCollection = { name ->
                val newCollId = container.favorites.createCollection(name)
                if (newCollId != null) {
                    if (favorited) {
                        container.favorites.moveToCollection(articleId, newCollId)
                    } else {
                        container.favorites.addFavorite(
                            articleId = articleId,
                            title = detailForDialog.title,
                            bookId = detailForDialog.bookId,
                            bookTitle = detailForDialog.bookTitle,
                            cat = "",
                            collectionId = newCollId,
                        )
                        favorited = true
                    }
                    currentCollId = newCollId
                    showCollDialog = false
                }
            },
            onRemove = {
                container.favorites.removeFavorite(articleId)
                favorited = false
                currentCollId = null
                showCollDialog = false
            },
        )
    }
}

/** 阅读页顶栏收藏按钮：已收藏为实心朱砂书签，未收藏为描边 */
@Composable
private fun FavoriteButton(favorited: Boolean, onClick: () -> Unit) {
    val tint = if (favorited) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick) {
        Icon(
            if (favorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = if (favorited) "管理收藏" else "添加到合集",
            tint = tint,
        )
    }
}

/**
 * 收藏 / 移动合集对话框。
 * - 未收藏时用于「添加到合集」：选择目标合集后调用 [onSelect] 新增收藏。
 * - 已收藏时用于「移动到合集」：选择目标合集后调用 [onSelect] 更新归属；
 *   另提供「取消收藏」入口（[onRemove]）。
 * 内置「+ 新建合集」行，创建后合集列表经 repo.version 自增自动刷新，
 * 用户可在同一次对话框内接着选中新合集完成收藏。
 */
@Composable
private fun AddToCollectionDialog(
    articleTitle: String,
    collections: List<Collection>,
    currentCollectionId: Long?,
    favorited: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
    onCreateCollection: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var showNewField by remember { mutableStateOf(false) }
    var newCollName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (favorited) "移动到合集" else "添加到合集") },
        text = {
            Column {
                Text(
                    "「$articleTitle」",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Column {
                    CollectionRow(
                        name = "未分组",
                        selected = currentCollectionId == null,
                        onClick = { onSelect(null) },
                    )
                    collections.forEach { c ->
                        CollectionRow(
                            name = c.name,
                            selected = currentCollectionId == c.id,
                            onClick = { onSelect(c.id) },
                        )
                    }
                    if (showNewField) {
                        OutlinedTextField(
                            value = newCollName,
                            onValueChange = { newCollName = it },
                            singleLine = true,
                            label = { Text("新合集名称") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        if (newCollName.isNotBlank()) {
                                            onCreateCollection(newCollName.trim())
                                            newCollName = ""
                                            showNewField = false
                                        }
                                    },
                                ) { Text("创建") }
                            },
                        )
                    } else {
                        CollectionRow(
                            name = "+ 新建合集",
                            selected = false,
                            isAddNew = true,
                            onClick = { showNewField = true },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (favorited) {
                TextButton(onClick = onRemove) { Text("取消收藏") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun CollectionRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    isAddNew: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isAddNew) Icons.Filled.Add
            else if (selected) Icons.Filled.Bookmark
            else Icons.Filled.BookmarkBorder,
            contentDescription = null,
            tint = if (selected || isAddNew) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 阅读页底部：上一篇 / 下一篇，整块大触控区；底栏下方还有全局常驻导航栏 */
@Composable
private fun ChapterNavBar(
    prevTitle: String?,
    nextTitle: String?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val hairline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = hairline,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1f),
                )
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(Modifier.fillMaxWidth().height(60.dp)) {
            ChapterNavItem(
                direction = "上一篇",
                title = prevTitle ?: "已是第一篇",
                enabled = prevTitle != null,
                onClick = onPrev,
                alignEnd = false,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .align(Alignment.CenterVertically)
                    .drawBehind { drawRect(hairline) },
            )
            ChapterNavItem(
                direction = "下一篇",
                title = nextTitle ?: "已是最后一篇",
                enabled = nextTitle != null,
                onClick = onNext,
                alignEnd = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChapterNavItem(
    direction: String,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        ) {
            if (!alignEnd) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
                Text(
                    direction,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (alignEnd) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
    if (enabled) {
        Surface(
            onClick = onClick,
            enabled = true,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier,
        ) { content() }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier,
        ) { content() }
    }
}
