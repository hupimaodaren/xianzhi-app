package org.xadfz.xianzhi.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.xadfz.xianzhi.db.WritableDb
import org.xadfz.xianzhi.db.openWritableDatabase
import org.xadfz.xianzhi.model.Collection
import org.xadfz.xianzhi.model.Favorite
import org.xadfz.xianzhi.util.userDataDirPath

/**
 * 收藏数据仓库：用户可写的 favorites.db，独立于只读志书主库。
 *
 * 一篇文章最多收藏一次（article_id UNIQUE），属于一个合集；
 * collection_id 为 NULL 表示「未分组」。删除合集时其下收藏自动移至未分组。
 *
 * [version] 每次写操作后自增，供 Compose UI 订阅刷新（`produceState(repo.version)`）。
 */
class FavoritesRepository(dbPath: String = "${userDataDirPath()}/favorites.db") {
    private val db: WritableDb = openWritableDatabase(dbPath)

    var version by mutableIntStateOf(0)
        private set

    init {
        db.exec("""
            CREATE TABLE IF NOT EXISTS collections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                sort INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.exec("""
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                article_id INTEGER NOT NULL UNIQUE,
                title TEXT NOT NULL,
                book_id TEXT NOT NULL,
                book_title TEXT NOT NULL,
                cat TEXT NOT NULL DEFAULT '',
                collection_id INTEGER,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE SET NULL
            )
        """.trimIndent())
        db.exec("CREATE INDEX IF NOT EXISTS idx_fav_coll ON favorites(collection_id)")
        db.exec("CREATE INDEX IF NOT EXISTS idx_fav_article ON favorites(article_id)")
        // SQLite 默认关闭外键，需显式开启，否则 ON DELETE SET NULL 不生效
        db.exec("PRAGMA foreign_keys=ON")
    }

    private fun bump() { version++ }

    /** 全部合集（按 sort、id 排序） */
    fun listCollections(): List<Collection> =
        db.query("SELECT id, name, sort FROM collections ORDER BY sort, id").map {
            Collection(it.getLong(0), it.getString(1).orEmpty(), it.getInt(2))
        }

    /** 新建合集；返回新合集 id，名称为空或重名时返回 null */
    fun createCollection(name: String): Long? {
        val n = name.trim()
        if (n.isEmpty()) return null
        return runCatching {
            val sort = db.query("SELECT COALESCE(MAX(sort),0)+1 FROM collections")
                .firstOrNull()?.getInt(0) ?: 1
            db.exec("INSERT INTO collections(name, sort) VALUES(?, ?)", n, sort)
            bump()
            db.query("SELECT id FROM collections WHERE name = ?", n).firstOrNull()?.getLong(0)
        }.getOrNull()
    }

    fun renameCollection(id: Long, name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        return runCatching {
            db.exec("UPDATE collections SET name = ? WHERE id = ?", n, id)
            bump()
            true
        }.getOrDefault(false)
    }

    /** 删除合集；其下收藏因 ON DELETE SET NULL 自动变为未分组 */
    fun deleteCollection(id: Long) {
        db.exec("DELETE FROM collections WHERE id = ?", id)
        bump()
    }

    /**
     * 列出收藏。
     * @param collectionId 合集 id；null=全部，0 或负数=未分组
     */
    fun listFavorites(collectionId: Long?): List<Favorite> {
        val sql = when {
            collectionId == null ->
                "SELECT id, article_id, title, book_id, book_title, cat, collection_id, created_at FROM favorites ORDER BY created_at DESC"
            collectionId <= 0 ->
                "SELECT id, article_id, title, book_id, book_title, cat, collection_id, created_at FROM favorites WHERE collection_id IS NULL ORDER BY created_at DESC"
            else ->
                "SELECT id, article_id, title, book_id, book_title, cat, collection_id, created_at FROM favorites WHERE collection_id = ? ORDER BY created_at DESC"
        }
        val rows = if (collectionId != null && collectionId > 0) db.query(sql, collectionId) else db.query(sql)
        return rows.map { r ->
            Favorite(
                id = r.getLong(0),
                articleId = r.getLong(1),
                title = r.getString(2).orEmpty(),
                bookId = r.getString(3).orEmpty(),
                bookTitle = r.getString(4).orEmpty(),
                cat = r.getString(5).orEmpty(),
                collectionId = r.getString(6)?.let { r.getLong(6) },
                createdAt = r.getLong(7),
            )
        }
    }

    fun isFavorited(articleId: Long): Boolean =
        db.query("SELECT 1 FROM favorites WHERE article_id = ?", articleId).isNotEmpty()

    fun getFavoriteByArticleId(articleId: Long): Favorite? =
        listFavorites(null).firstOrNull { it.articleId == articleId }

    /** 收藏到指定合集；collectionId=null 表示未分组。已收藏则更新合集归属。 */
    fun addFavorite(
        articleId: Long,
        title: String,
        bookId: String,
        bookTitle: String,
        cat: String,
        collectionId: Long? = null,
    ) {
        val now = System.currentTimeMillis()
        // 已存在则更新合集与时间（保证再次收藏刷新排序）
        val exists = db.query("SELECT id FROM favorites WHERE article_id = ?", articleId).firstOrNull()
        if (exists != null) {
            db.exec(
                "UPDATE favorites SET collection_id = ?, created_at = ? WHERE article_id = ?",
                collectionId, now, articleId,
            )
        } else {
            db.exec(
                "INSERT INTO favorites(article_id, title, book_id, book_title, cat, collection_id, created_at) VALUES(?,?,?,?,?,?,?)",
                articleId, title, bookId, bookTitle, cat, collectionId, now,
            )
        }
        bump()
    }

    fun removeFavorite(articleId: Long) {
        db.exec("DELETE FROM favorites WHERE article_id = ?", articleId)
        bump()
    }

    /** 切换收藏状态：未收藏→加入未分组；已收藏→移除。返回操作后的收藏状态。 */
    fun toggleFavorite(
        articleId: Long,
        title: String,
        bookId: String,
        bookTitle: String,
        cat: String,
    ): Boolean {
        return if (isFavorited(articleId)) {
            removeFavorite(articleId)
            false
        } else {
            addFavorite(articleId, title, bookId, bookTitle, cat, collectionId = null)
            true
        }
    }

    /** 把某篇收藏移动到指定合集；collectionId=null 移至未分组 */
    fun moveToCollection(articleId: Long, collectionId: Long?) {
        db.exec("UPDATE favorites SET collection_id = ? WHERE article_id = ?", collectionId, articleId)
        bump()
    }

    fun close() = db.close()
}
