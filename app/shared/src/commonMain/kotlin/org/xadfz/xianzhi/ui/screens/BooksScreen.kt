package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.model.Article
import org.xadfz.xianzhi.model.Category
import org.xadfz.xianzhi.ui.AppNavigator
import org.xadfz.xianzhi.ui.Route
import org.xadfz.xianzhi.util.ioDispatcher

@Composable
fun BooksTab(container: AppContainer, navigator: AppNavigator, modifier: Modifier = Modifier) {
    when (val route = navigator.current) {
        is Route.ArticleList -> ArticleListScreen(
            container = container,
            bookId = route.bookId,
            bookTitle = route.bookTitle,
            onBack = { navigator.back() },
            onOpenArticle = { id -> navigator.push(Route.Reader(id)) },
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
        topBar = { TopAppBar(title = { Text("西安地方志") }) },
    ) { padding ->
        if (categories == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categories!!, key = { it.name }) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenCat(cat.name) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(cat.name, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "${cat.books.size} 部 · ${cat.articleCount} 篇",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
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
                title = { Text(cat) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        if (books == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(books!!, key = { it.id }) { book ->
                Column(
                    Modifier.fillMaxWidth().clickable { onOpenBook(book.id, book.title) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${book.n} 篇",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
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
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(bookTitle, maxLines = 1) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        if (articles == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        // 按 vol（章/部/栏目）分组展示
        val grouped = articles!!.groupBy { it.vol.ifBlank { "" } }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            grouped.forEach { (vol, list) ->
                if (vol.isNotBlank()) {
                    item(key = "vol-$vol") {
                        Text(
                            vol,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                }
                items(list, key = { it.id }) { a ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onOpenArticle(a.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(a.title, style = MaterialTheme.typography.bodyLarge)
                        if (a.date.isNotBlank()) {
                            Text(
                                a.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
