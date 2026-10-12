package org.xadfz.xianzhi.db

import org.xadfz.xianzhi.model.Article
import org.xadfz.xianzhi.model.ArticleDetail
import org.xadfz.xianzhi.model.Book
import org.xadfz.xianzhi.model.Category
import org.xadfz.xianzhi.model.Neighbor
import org.xadfz.xianzhi.model.SearchHit

/**
 * 全部数据查询，SQLite 内核两端通用。
 */
class Queries(private val db: Db) {

    /** 首页六栏目（含栏目下书目，按 books.id 排序） */
    fun categories(): List<Category> {
        val rows = db.query("SELECT cat, id, title, n FROM books ORDER BY id")
        val byCat = LinkedHashMap<String, MutableList<Book>>()
        for (r in rows) {
            val cat = r.getString(0).orEmpty()
            byCat.getOrPut(cat) { mutableListOf() }.add(
                Book(id = r.getString(1).orEmpty(), title = r.getString(2).orEmpty(),
                    cat = cat, n = r.getInt(3)),
            )
        }
        return DbSchema.CATEGORY_ORDER.filter { byCat.containsKey(it) }
            .map { Category(it, byCat.getValue(it)) }
    }

    /** 某书目的篇目列表（不取 body，快） */
    fun articlesOfBook(bookId: String): List<Article> =
        db.query(
            "SELECT id, title, vol, date FROM arts WHERE book = ? ORDER BY id",
            bookId,
        ).map {
            Article(
                id = it.getLong(0),
                title = it.getString(1).orEmpty(),
                vol = it.getString(2).orEmpty(),
                date = it.getString(3).orEmpty(),
            )
        }

    /** 阅读一篇（含所属书名与正文） */
    fun articleDetail(id: Long): ArticleDetail? {
        val r = db.query(
            """
            SELECT a.id, a.book, b.title, a.title, a.vol, a.date, a.body
            FROM arts a JOIN books b ON b.id = a.book
            WHERE a.id = ?
            """.trimIndent(),
            id,
        ).firstOrNull() ?: return null
        return ArticleDetail(
            id = r.getLong(0),
            bookId = r.getString(1).orEmpty(),
            bookTitle = r.getString(2).orEmpty(),
            title = r.getString(3).orEmpty(),
            vol = r.getString(4).orEmpty(),
            date = r.getString(5).orEmpty(),
            body = r.getString(6).orEmpty(),
        )
    }

    /** 同书内上一篇/下一篇 */
    fun neighbor(bookId: String, id: Long, next: Boolean): Neighbor? {
        val op = if (next) ">" else "<"
        val order = if (next) "ASC" else "DESC"
        val r = db.query(
            "SELECT id, title FROM arts WHERE book = ? AND id $op ? ORDER BY id $order LIMIT 1",
            bookId, id,
        ).firstOrNull() ?: return null
        return Neighbor(r.getLong(0), r.getString(1).orEmpty())
    }

    /**
     * 全文检索：instr 全表扫描（约 1-3 秒），标题命中优先，最多 200 条。
     */
    fun search(keyword: String, limit: Int = 200): List<SearchHit> {
        if (keyword.isBlank()) return emptyList()
        return db.query(
            """
            SELECT a.id, a.title, a.book, b.title AS book_title, b.cat
            FROM arts a JOIN books b ON b.id = a.book
            WHERE instr(a.body, ?) > 0 OR instr(a.title, ?) > 0
            ORDER BY (instr(a.title, ?) > 0) DESC, a.id LIMIT ?
            """.trimIndent(),
            keyword, keyword, keyword, limit,
        ).map {
            SearchHit(
                id = it.getLong(0),
                title = it.getString(1).orEmpty(),
                book = it.getString(2).orEmpty(),
                bookTitle = it.getString(3).orEmpty(),
                cat = it.getString(4).orEmpty(),
            )
        }
    }

    /** 命中篇目数（检索框输入时的预览） */
    fun countHits(keyword: String): Int {
        if (keyword.isBlank()) return 0
        return db.query(
            "SELECT count(*) FROM arts WHERE instr(body, ?) > 0 OR instr(title, ?) > 0",
            keyword, keyword,
        ).firstOrNull()?.getInt(0) ?: 0
    }

    fun bookTitle(bookId: String): String? =
        db.query("SELECT title FROM books WHERE id = ?", bookId)
            .firstOrNull()?.getString(0)

    fun totalBooks(): Int =
        db.query("SELECT count(*) FROM books").firstOrNull()?.getInt(0) ?: 0

    fun totalArticles(): Int =
        db.query("SELECT count(*) FROM arts").firstOrNull()?.getInt(0) ?: 0
}
