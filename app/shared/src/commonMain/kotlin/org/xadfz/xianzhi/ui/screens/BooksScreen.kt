package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.xadfz.xianzhi.ui.CountBadge
import org.xadfz.xianzhi.ui.LocalIcons
import org.xadfz.xianzhi.ui.Route
import org.xadfz.xianzhi.ui.SealBadge
import org.xadfz.xianzhi.ui.SectionLabel
import org.xadfz.xianzhi.ui.comfortableSidePadding
import org.xadfz.xianzhi.util.ioDispatcher

@Composable
fun BooksTab(container: AppContainer, navigator: AppNavigator, modifier: Modifier = Modifier) {
    when (val route = navigator.current) {
        is Route.ArticleList -> ArticleListScreen(
            container = container,
            bookId = route.bookId,
            bookTitle = route.bookTitle,
            listState = navigator.listState("al-${route.bookId}"),
            onBack = { navigator.back() },
            onOpenArticle = { id -> navigator.openChapter(id) },
            modifier = modifier,
        )
        is Route.BookList -> BookListScreen(
            container = container,
            cat = route.cat,
            listState = navigator.listState("bl-${route.cat}"),
            onBack = { navigator.back() },
            onOpenBook = { id, title -> navigator.push(Route.ArticleList(id, title)) },
            modifier = modifier,
        )
        else -> CategoryScreen(
            container = container,
            listState = navigator.listState("cat"),
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
    listState: LazyListState,
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "hero") {
                    CategoryHero(categories!!)
                }
                itemsIndexed(categories!!, key = { _, cat -> cat.name }) { index, cat ->
                    CategoryCard(
                        index = index,
                        cat = cat,
                        onClick = { onOpenCat(cat.name) },
                    )
                }
            }
        }
    }
}

/**
 * 书目页首屏题头：衬线大标题 + 一句说明 + 三格统计，
 * 把「有多少内容」这件事提到最前面，避免首屏只有一串同样的卡片。
 */
@Composable
private fun CategoryHero(categories: List<Category>) {
    val books = remember(categories) { categories.sumOf { it.books.size } }
    val articles = remember(categories) { categories.sumOf { it.articleCount } }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
        Text(
            "地方志总目",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "依书目分类分卷，逐层展开至篇目；全文离线可检索",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroStat("${categories.size}", "类目", Modifier.weight(1f))
            HeroStat("$books", "部书", Modifier.weight(1f))
            HeroStat("$articles", "篇目", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 分类卡片：左侧印章式序号，中间分类名与统计（部/篇用淡底标签并列），
 * 右侧箭头在悬停/按压时向朱砂色过渡，形成「典籍书脊」的一列视觉节奏。
 */
@Composable
private fun CategoryCard(index: Int, cat: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SealBadge(
                text = CN_NUMERALS.getOrElse(index) { "${index + 1}" },
                size = 40.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    cat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CountBadge("${cat.books.size} 部")
                    Spacer(Modifier.width(6.dp))
                    CountBadge("${cat.articleCount} 篇")
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** 分类序号用汉字，与志书目录的传统计数一致；超过十二类时退回阿拉伯数字 */
private val CN_NUMERALS = listOf(
    "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookListScreen(
    container: AppContainer,
    cat: String,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenBook: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val books by produceState<List<org.xadfz.xianzhi.model.Book>?>(null, cat) {
        value = withContext(ioDispatcher) {
            container.queries.booksOfCategory(cat)
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
            val total = remember(books) { books!!.sumOf { it.n } }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, top = 4.dp, bottom = 24.dp),
            ) {
                item(key = "cat-summary") {
                    // 区段摘要：分类名之外先给出「合多少部、多少篇」的总量，
                    // 方便读者在进入目录前对自己要翻的书有个体量概念
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("共 ${books!!.size} 部")
                        Spacer(Modifier.weight(1f))
                        CountBadge("${total} 篇")
                    }
                }
                itemsIndexed(books!!, key = { _, b -> b.id }) { i, book ->
                    BookRow(
                        index = i,
                        title = book.title,
                        count = book.n,
                        onClick = { onOpenBook(book.id, book.title) },
                    )
                }
            }
        }
    }
}

/** 书目行：等宽序号 + 书名 + 篇数标签 + 箭头，行间以细线分隔，保持列表的轻量感 */
@Composable
private fun BookRow(index: Int, title: String, count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        CountBadge("$count 篇")
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 14.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleListScreen(
    container: AppContainer,
    bookId: String,
    bookTitle: String,
    listState: LazyListState,
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
        // Scaffold 用 SubcomposeLayout，内容 lambda 在子组合中晚于外层组合执行：
        // 期间若数据刚到，闭包里捕获的 toc 仍是上一帧的 null，这里以实时的 articles 兜底
        val readyToc = toc
        if (articles == null || readyToc == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val flat = remember(readyToc, expanded) { flattenToc(readyToc, expanded) }
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = comfortableSidePadding(maxContent = 860.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = side, end = side, bottom = 12.dp),
            ) {
                // 目录摘要：与书目页同名目统计样式，进入目录即可看到规模
                item(key = "toc-summary") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("全书目录")
                        Spacer(Modifier.weight(1f))
                        CountBadge("${articles?.size ?: 0} 篇")
                    }
                }
                items(flat, key = { it.key }) { node ->
                    when (node) {
                        is TocLeaf -> {
                            val a = node.article
                            // 层级缩进 + 左侧细导引线：多级目录能一眼看出归属，浅色线不抢正文
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenArticle(a.id) }
                                    .padding(start = (14 + (node.level - 1) * 16).dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (node.level > 1) {
                                    Box(
                                        Modifier
                                            .size(width = 2.dp, height = 14.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(a.title, style = MaterialTheme.typography.bodyLarge)
                                    if (a.date.isNotBlank()) {
                                        Text(
                                            a.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = (14 + (node.level - 1) * 16).dp, end = 14.dp),
                            )
                        }
                        is TocGroup -> {
                            val open = node.key in expanded
                            // 分组行以淡底色与正文行区分，层级越深底色越淡，
                            // 折叠箭头用矢量图标代替字符，缩放与对齐在两端一致
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (open) MaterialTheme.colorScheme.surfaceContainer
                                        else MaterialTheme.colorScheme.surface,
                                    )
                                    .clickable {
                                        updateExpanded(
                                            if (open) expanded - node.key else expanded + node.key,
                                        )
                                    }
                                    .padding(start = (10 + (node.level - 1) * 16).dp, end = 12.dp,
                                        top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (open) LocalIcons.ExpandLess else LocalIcons.ExpandMore,
                                    contentDescription = if (open) "收起" else "展开",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
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
                                        modifier = Modifier.size(30.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "读本篇",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    CountBadge("${node.leafCount}")
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 14.dp),
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
