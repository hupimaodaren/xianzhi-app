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
 * [MAX_ENTRIES] 条，超出后最旧记录自动淘汰。排序按阅读时间倒序；时间戳由
 * [nextStamp] 保证严格递增，因此同毫秒内的连续写入也能排出确定先后。
 *
 * [version] 每次写操作后自增，供 Compose UI 订阅刷新（`produceState(repo.version)`）。
 */
class HistoryRepository(dbPath: String = "${userDataDirPath()}/history.db") {
    private val db: WritableDb = openWritableDatabase(dbPath)

    var version by mutableIntStateOf(0)
        private set

    /** 最近一次写入使用的时间戳，保证同毫秒内多次写入也能排出先后 */
    private var lastStamp: Long = 0L

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
        lastStamp = db.query("SELECT COALESCE(MAX(last_read_at), 0) FROM reading_history")
            .firstOrNull()?.getLong(0) ?: 0L
    }

    private fun bump() { version++ }

    /**
     * 下一次写入使用的时间戳：取当前毫秒与「上一次写入 + 1」的较大值。
     *
     * 主键是 article_id，rowid 与 article_id 同值，无法再用 rowid 决胜写入先后；
     * 而连续 record（尤其重读同一篇）常落在同一毫秒内，只靠 last_read_at 排序会让
     * 「刚读过的那篇」排不到最前。这里用单调递增的时间戳保证顺序确定。
     */
    private fun nextStamp(): Long {
        val now = maxOf(System.currentTimeMillis(), lastStamp + 1)
        lastStamp = now
        return now
    }

    /** 记录一次阅读；同篇重读更新为最新时间（排序置顶），随后按上限淘汰最旧记录 */
    fun record(articleId: Long, title: String, bookId: String, bookTitle: String) {
        val now = nextStamp()
        db.exec(
            "INSERT OR REPLACE INTO reading_history(article_id, title, book_id, book_title, last_read_at) VALUES(?,?,?,?,?)",
            articleId, title, bookId, bookTitle, now,
        )
        db.exec(
            "DELETE FROM reading_history WHERE article_id NOT IN " +
                "(SELECT article_id FROM reading_history ORDER BY last_read_at DESC LIMIT $MAX_ENTRIES)",
        )
        bump()
    }

    /** 全部历史，按最近阅读时间倒序 */
    fun list(): List<HistoryEntry> =
        db.query(
            "SELECT article_id, title, book_id, book_title, last_read_at " +
                "FROM reading_history ORDER BY last_read_at DESC",
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
