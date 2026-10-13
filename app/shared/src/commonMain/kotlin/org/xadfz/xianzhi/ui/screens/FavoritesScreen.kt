package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.model.Collection
import org.xadfz.xianzhi.model.Favorite
import org.xadfz.xianzhi.ui.AppNavigator
import org.xadfz.xianzhi.ui.comfortableSidePadding
import org.xadfz.xianzhi.util.ioDispatcher

/** 合集过滤：null=全部，-1=未分组，其余为合集 id */
private data class CollFilter(val label: String, val id: Long?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    container: AppContainer,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val repo = container.favorites

    val collections by produceState<List<Collection>>(emptyList(), repo.version) {
        value = withContext(ioDispatcher) { repo.listCollections() }
    }
    val filters = remember(collections) {
        listOf(CollFilter("全部", null), CollFilter("未分组", -1L)) +
            collections.map { CollFilter(it.name, it.id) }
    }

    var selectedId by remember { mutableStateOf<Long?>(null) }
    val favorites by produceState<List<Favorite>>(emptyList(), repo.version, selectedId) {
        value = withContext(ioDispatcher) { repo.listFavorites(selectedId) }
    }

    var showNewColl by remember { mutableStateOf(false) }
    var renamingColl by remember { mutableStateOf<Collection?>(null) }
    var movingFavorite by remember { mutableStateOf<Favorite?>(null) }

    val selectedColl = remember(collections, selectedId) {
        collections.firstOrNull { it.id == selectedId }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("收藏") },
                actions = {
                    selectedColl?.let { coll ->
                        IconButton(onClick = { renamingColl = coll }) {
                            Icon(
                                Icons.Filled.DriveFileRenameOutline,
                                contentDescription = "重命名合集",
                            )
                        }
                        IconButton(onClick = {
                            repo.deleteCollection(coll.id)
                            selectedId = null
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除合集")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = comfortableSidePadding(maxContent = 860.dp)
            Column(Modifier.fillMaxSize()) {
                // 合集选择行
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = side, end = side, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    filters.forEach { f ->
                        FilterChip(
                            selected = selectedId == f.id,
                            onClick = { selectedId = f.id },
                            label = { Text(f.label) },
                        )
                    }
                    IconButton(onClick = { showNewColl = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建合集")
                    }
                }

                if (favorites.isEmpty()) {
                    EmptyFavorites(
                        hasCollections = collections.isNotEmpty(),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = side, end = side, top = 4.dp, bottom = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(favorites, key = { it.id }) { fav ->
                            FavoriteItem(
                                fav = fav,
                                allCollections = collections,
                                onClick = { navigator.openChapter(fav.articleId) },
                                onMove = { movingFavorite = fav },
                                onRemove = {
                                    repo.removeFavorite(fav.articleId)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewColl) {
        CollectionNameDialog(
            title = "新建合集",
            initial = "",
            onDismiss = { showNewColl = false },
            onConfirm = { name ->
                repo.createCollection(name)
                showNewColl = false
            },
        )
    }
    renamingColl?.let { coll ->
        CollectionNameDialog(
            title = "重命名合集",
            initial = coll.name,
            onDismiss = { renamingColl = null },
            onConfirm = { name ->
                repo.renameCollection(coll.id, name)
                renamingColl = null
            },
        )
    }
    movingFavorite?.let { fav ->
        MoveCollectionDialog(
            favorite = fav,
            collections = collections,
            onDismiss = { movingFavorite = null },
            onConfirm = { collId ->
                repo.moveToCollection(fav.articleId, collId)
                movingFavorite = null
            },
        )
    }
}

@Composable
private fun FavoriteItem(
    fav: Favorite,
    allCollections: List<Collection>,
    onClick: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    fav.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (fav.cat.isBlank()) fav.bookTitle else "${fav.cat} · ${fav.bookTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("移动到合集") },
                        onClick = { menuOpen = false; onMove() },
                    )
                    DropdownMenuItem(
                        text = { Text("取消收藏") },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("合集名称") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.trim().isNotEmpty()) onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun MoveCollectionDialog(
    favorite: Favorite,
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到合集") },
        text = {
            Column {
                Text(
                    "「${favorite.title}」",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Column {
                    MoveOption("未分组", selected = favorite.collectionId == null) {
                        onConfirm(null)
                    }
                    collections.forEach { c ->
                        MoveOption(c.name, selected = favorite.collectionId == c.id) {
                            onConfirm(c.id)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun MoveOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyFavorites(hasCollections: Boolean, modifier: Modifier = Modifier) {
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
                        Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "还没有收藏",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasCollections) "阅读时点击书签图标即可收藏"
                else "阅读时点击书签图标收藏，可在此新建合集分组",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
