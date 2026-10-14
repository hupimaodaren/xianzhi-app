package org.xadfz.xianzhi.db

import org.xadfz.xianzhi.model.Article
import org.xadfz.xianzhi.model.ArticleDetail
import org.xadfz.xianzhi.model.Book
import org.xadfz.xianzhi.model.Category
import org.xadfz.xianzhi.model.Neighbor
import org.xadfz.xianzhi.model.SearchHit
import org.xadfz.xianzhi.search.SearchSnippet

/**
 * 全部数据查询，SQLite 内核两端通用。
 *
 * 库格式版本 2：正文以 zlib 压缩块（blocks 表）存储，arts 只存正文位置
 * （blk/off/nb）。阅读取一篇解压单块（约 1ms）；全文检索由 SQL 提供元数据、
 * 客户端解压块后在内存中做子串匹配——磁盘 IO 从约 154MB 降到约 55MB，
 * 语义与旧版 SQL instr 完全一致（大小写敏感子串、标题命中优先、再按 id）。
 */
class Queries(private val db: Db) {

    private val bodies = BodyReader(db)

    /** 最近一次全库扫描的结果缓存：countHits → search → searchPage 连续调用只扫一次 */
    private var scanCache: Scan? = null

    /** 首页六栏目（含栏目下书目，按 seq 排序保证中文卷序正确） */
    fun categories(): List<Category> {
        val rows = db.query("SELECT cat, id, title, n FROM books ORDER BY seq, id")
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

    /** 某书目的篇目列表（不取正文，快） */
    fun articlesOfBook(bookId: String): List<Article> =
        db.query(
            "SELECT id, title, vol, date, path FROM arts WHERE book = ? ORDER BY id",
            bookId,
        ).map {
            Article(
                id = it.getLong(0),
                title = it.getString(1).orEmpty(),
                vol = it.getString(2).orEmpty(),
                date = it.getString(3).orEmpty(),
                path = it.getString(4).orEmpty(),
            )
        }

    /** 阅读一篇（含所属书名与正文） */
    fun articleDetail(id: Long): ArticleDetail? {
        val r = db.query(
            """
            SELECT a.id, a.book, b.title, a.title, a.vol, a.date, a.blk, a.off, a.nb
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
            body = bodies.read(r.getInt(6), r.getInt(7), r.getInt(8)).decodeToString(),
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
     * 全文检索：客户端全库扫描（解压正文块后子串匹配），标题命中优先。
     * [limit] 全局最多返回条数（结果页按书目大分类分组展示）；
     * 每条携带命中行及其前后各 2 行上下文。
     */
    fun search(keyword: String, limit: Int = SEARCH_MAX_HITS): List<SearchHit> =
        searchPage(keyword, 0, limit)

    /**
     * 分批检索：与 [search] 同序（标题命中优先，再按 id），从 [offset] 起取 [limit] 条。
     * 阈值突破后分批续查并用已完成条数展示进度。
     */
    fun searchPage(keyword: String, offset: Int, limit: Int): List<SearchHit> {
        if (keyword.isBlank() || limit <= 0 || offset < 0) return emptyList()
        val hits = scan(keyword)
        val from = offset.coerceAtMost(hits.size)
        val to = (offset + limit).coerceAtMost(hits.size)
        return hits.subList(from, to).map { it.toSearchHit(keyword) }
    }

    companion object {
        /** 检索结果全局上限 */
        const val SEARCH_MAX_HITS = 500

        /** 阈值续查时每批取回的条数（每批复用同一次全库扫描的结果，批越大总耗时越短） */
        const val SEARCH_FETCH_BATCH = 500
    }

    /** 命中篇目数（检索框输入时的预览） */
    fun countHits(keyword: String): Int {
        if (keyword.isBlank()) return 0
        return scan(keyword).size
    }

    fun bookTitle(bookId: String): String? =
        db.query("SELECT title FROM books WHERE id = ?", bookId)
            .firstOrNull()?.getString(0)

    fun totalBooks(): Int =
        db.query("SELECT count(*) FROM books").firstOrNull()?.getInt(0) ?: 0

    fun totalArticles(): Int =
        db.query("SELECT count(*) FROM arts").firstOrNull()?.getInt(0) ?: 0

    /** 检索命中的轻量候选：正文按需解压，仅返回条目才构建片段 */
    private class Hit(
        val id: Long,
        val title: String,
        val book: String,
        val bookTitle: String,
        val cat: String,
        val blk: Int,
        val off: Int,
        val nb: Int,
        val titleHit: Boolean,
    )

    private class Scan(val keyword: String, val hits: List<Hit>)

    private fun Hit.toSearchHit(keyword: String): SearchHit = SearchHit(
        id = id,
        title = title,
        book = book,
        bookTitle = bookTitle,
        cat = cat,
        snippet = SearchSnippet.build(
            bodies.read(blk, off, nb).decodeToString(), keyword,
        ),
    )

    /**
     * 全库扫描：按 id 序遍历元数据，标题在 SQL 侧匹配，正文解压后在内存匹配。
     * 结果与旧版 `WHERE instr(body,kw)>0 OR instr(title,kw)>0
     * ORDER BY (instr(title,kw)>0) DESC, id` 逐条一致。
     */
    private fun scan(keyword: String): List<Hit> {
        scanCache?.let { if (it.keyword == keyword) return it.hits }
        val needle = keyword.encodeToByteArray()
        val rows = db.query(
            """
            SELECT a.id, a.title, a.book, b.title, b.cat, a.blk, a.off, a.nb
            FROM arts a JOIN books b ON b.id = a.book
            ORDER BY a.id
            """.trimIndent(),
        )
        val titleHits = ArrayList<Hit>()
        val bodyHits = ArrayList<Hit>()
        for (r in rows) {
            val id = r.getLong(0)
            val title = r.getString(1).orEmpty()
            val blk = r.getInt(5)
            val off = r.getInt(6)
            val nb = r.getInt(7)
            val hit = if (keyword in title) {
                Hit(id, title, r.getString(2).orEmpty(), r.getString(3).orEmpty(),
                    r.getString(4).orEmpty(), blk, off, nb, titleHit = true)
            } else {
                if (!bodies.contains(blk, off, nb, needle)) continue
                Hit(id, title, r.getString(2).orEmpty(), r.getString(3).orEmpty(),
                    r.getString(4).orEmpty(), blk, off, nb, titleHit = false)
            }
            (if (hit.titleHit) titleHits else bodyHits).add(hit)
        }
        val hits = titleHits + bodyHits
        scanCache = Scan(keyword, hits)
        return hits
    }
}
