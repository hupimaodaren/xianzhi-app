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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.AppInfo
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.model.Article
import org.xadfz.xianzhi.model.Category
import org.xadfz.xianzhi.model.TocExpansionMemory
import org.xadfz.xianzhi.model.TocGroup
import org.xadfz.xianzhi.model.TocLeaf
import org.xadfz.xianzhi.model.allGroupKeys
import org.xadfz.xianzhi.model.buildToc
import org.xadfz.xianzhi.model.flattenToc
import org.xadfz.xianzhi.ui.AppNavigator
import org.xadfz.xianzhi.ui.Route
import org.xadfz.xianzhi.ui.comfortableSidePadding
import org.xadfz.xianzhi.util.ioDispatcher

@Composable
fun BooksTab(container: AppContainer, navigator: AppNavigator, modifier: Modifier = Modifier) {
    when (val route = navigator.current) {
        is Route.ArticleList -> ArticleListScreen(
            container = container,
            bookId = route.bookId,
            bookTitle = route.bookTitle,
            onBack = { navigator.back() },
            onOpenArticle = { id -> navigator.openChapter(id) },
            modifier = modifier,
        )
        is Route.BookList -> BookListScreen(
            container = container,
            cat = route.cat,
            onBack = { navigator.back() },
            onOpenBook = { id, title -> navigator.push(Route.ArticleList(id, title)) },
            modifier = modifier,
        )
        else -> CategoryScreen(
            container = container,
            onOpenCat = { cat -> navigator.push(Route.BookList(cat)) },
            modifier = modifier,
        )
    }
}

/** 顶部栏标题：朱砂印章小方 + 衬线书名，形成品牌记忆点 */
@Composable
fun BrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .padding(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "志",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                AppInfo.APP_NAME,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "地方志全文离线阅读",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryScreen(
    container: AppContainer,
    onOpenCat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories by produceState<List<Category>?>(null) {
        value = withContext(ioDispatcher) { container.queries.categories() }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { BrandTitle() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (categories == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = comfortableSidePadding(maxContent = 860.dp)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(categories!!, key = { it.name }) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCat(cat.name) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${cat.books.size} 部 · ${cat.articleCount} 篇",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookListScreen(
    container: AppContainer,
    cat: String,
    onBack: () -> Unit,
    onOpenBook: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val books by produceState<List<org.xadfz.xianzhi.model.Book>?>(null, cat) {
        value = withContext(ioDispatcher) {
            container.queries.categories().firstOrNull { it.name == cat }?.books
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(cat, maxLines = 1) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (books == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = comfortableSidePadding(maxContent = 860.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side),
            ) {
                items(books!!, key = { it.id }) { book ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBook(book.id, book.title) }
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(book.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${book.n} 篇",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleListScreen(
    container: AppContainer,
    bookId: String,
    bookTitle: String,
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val articles by produceState<List<Article>?>(null, bookId) {
        value = withContext(ioDispatcher) { container.queries.articlesOfBook(bookId) }
    }
    // 目录与展开状态需同时供顶栏「全展开/全收起」与内容列表使用，提升到 Scaffold 之外
    val toc = remember(articles) { articles?.let { buildToc(it) } }
    val allKeys = remember(toc) { toc?.let { allGroupKeys(it) } ?: emptySet() }
    var expanded by remember(bookId, toc) {
        mutableStateOf(toc?.let { TocExpansionMemory.load(bookId, it) } ?: emptySet())
    }
    fun updateExpanded(keys: Set<String>) {
        expanded = keys
        TocExpansionMemory.save(bookId, keys)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(bookTitle, maxLines = 1) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    // 多级目录：任意层级一键全部展开 / 全部收起
                    TextButton(
                        onClick = { updateExpanded(allKeys) },
                        enabled = expanded.size < allKeys.size,
                    ) { Text("全展开") }
                    TextButton(
                        onClick = { updateExpanded(emptySet()) },
                        enabled = expanded.isNotEmpty(),
                    ) { Text("全收起") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (articles == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val readyToc = toc!!
        val flat = remember(readyToc, expanded) { flattenToc(readyToc, expanded) }
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = comfortableSidePadding(maxContent = 860.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, bottom = 12.dp),
            ) {
                items(flat, key = { it.key }) { node ->
                    when (node) {
                        is TocLeaf -> {
                            val a = node.article
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenArticle(a.id) }
                                    .padding(start = (8 + (node.level - 1) * 14).dp, end = 18.dp, top = 13.dp, bottom = 13.dp),
                            ) {
                                Text(a.title, style = MaterialTheme.typography.bodyLarge)
                                if (a.date.isNotBlank()) {
                                    Text(
                                        a.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 18.dp),
                            )
                        }
                        is TocGroup -> {
                            val open = node.key in expanded
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        updateExpanded(
                                            if (open) expanded - node.key else expanded + node.key,
                                        )
                                    }
                                    .padding(start = (8 + (node.level - 1) * 14).dp, end = 18.dp, top = 11.dp, bottom = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (open) "▾" else "▸",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    node.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (node.articleId != null) {
                                    // 该层级本身也是一篇文章：标题行负责展开，箭头负责读正文
                                    IconButton(
                                        onClick = { onOpenArticle(node.articleId) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "读本篇",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    Text(
                                        "${node.leafCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上一级")
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
