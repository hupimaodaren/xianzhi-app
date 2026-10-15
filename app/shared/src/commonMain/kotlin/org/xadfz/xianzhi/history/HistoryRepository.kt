package org.xadfz.xianzhi.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.xadfz.xianzhi.db.WritableDb
import org.xadfz.xianzhi.db.openWritableDatabase
import org.xadfz.xianzhi.util.userDataDirPath

/** 一条阅读历史（某篇志书篇目最近一次的阅读记录） */
data class HistoryEntry(
    val articleId: Long,
    val title: String,
    val bookId: String,
    val bookTitle: String,
    val lastReadAt: Long,
)

/**
 * 阅读历史仓库：用户可写的 history.db，独立于只读志书主库与收藏库。
 *
 * 每篇只保留最近一次记录（article_id 主键去重，重读自动置顶），总量封顶
 * [MAX_ENTRIES] 条，超出后最旧记录自动淘汰。排序按阅读时间倒序，同毫秒
 * 写入以 rowid（写入先后）决胜，保证快速连续写入时顺序确定。
 *
 * [version] 每次写操作后自增，供 Compose UI 订阅刷新（`produceState(repo.version)`）。
 */
class HistoryRepository(dbPath: String = "${userDataDirPath()}/history.db") {
    private val db: WritableDb = openWritableDatabase(dbPath)

    var version by mutableIntStateOf(0)
        private set

    init {
        db.exec(
            """
            CREATE TABLE IF NOT EXISTS reading_history (
                article_id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                book_id TEXT NOT NULL,
                book_title TEXT NOT NULL,
                last_read_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun bump() { version++ }

    /** 记录一次阅读；同篇重读更新为最新时间（排序置顶），随后按上限淘汰最旧记录 */
    fun record(articleId: Long, title: String, bookId: String, bookTitle: String) {
        val now = System.currentTimeMillis()
        db.exec(
            "INSERT OR REPLACE INTO reading_history(article_id, title, book_id, book_title, last_read_at) VALUES(?,?,?,?,?)",
            articleId, title, bookId, bookTitle, now,
        )
        db.exec(
            "DELETE FROM reading_history WHERE article_id NOT IN " +
                "(SELECT article_id FROM reading_history ORDER BY last_read_at DESC, rowid DESC LIMIT $MAX_ENTRIES)",
        )
        bump()
    }

    /** 全部历史，按最近阅读时间倒序 */
    fun list(): List<HistoryEntry> =
        db.query(
            "SELECT article_id, title, book_id, book_title, last_read_at " +
                "FROM reading_history ORDER BY last_read_at DESC, rowid DESC",
        ).map { r ->
            HistoryEntry(
                articleId = r.getLong(0),
                title = r.getString(1).orEmpty(),
                bookId = r.getString(2).orEmpty(),
                bookTitle = r.getString(3).orEmpty(),
                lastReadAt = r.getLong(4),
            )
        }

    fun count(): Int =
        db.query("SELECT COUNT(*) FROM reading_history").firstOrNull()?.getInt(0) ?: 0

    fun remove(articleId: Long) {
        db.exec("DELETE FROM reading_history WHERE article_id = ?", articleId)
        bump()
    }

    fun clear() {
        db.exec("DELETE FROM reading_history")
        bump()
    }

    fun close() = db.close()

    companion object {
        const val MAX_ENTRIES = 200
    }
}
