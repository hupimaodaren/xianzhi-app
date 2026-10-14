package org.xadfz.xianzhi.db

import org.xadfz.xianzhi.db.openWritableDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueriesSearchPageTest {

    private lateinit var dbPath: String
    private lateinit var queries: Queries
    private lateinit var db: WritableDb

    @BeforeTest
    fun setup() {
        val tmp = File(System.getProperty("java.io.tmpdir"), "xz_search_test_${System.nanoTime()}.db")
        dbPath = tmp.absolutePath
        db = openWritableDatabase(dbPath)
        db.exec("CREATE TABLE meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.exec("INSERT INTO meta VALUES ('format_version', '2')")
        db.exec("CREATE TABLE books (id TEXT PRIMARY KEY, title TEXT, cat TEXT, seq INTEGER, n INTEGER)")
        db.exec(
            "CREATE TABLE arts (" +
                "id INTEGER PRIMARY KEY, title TEXT, vol TEXT, date TEXT, path TEXT, " +
                "book TEXT, blk INT NOT NULL, off INT NOT NULL, nb INT NOT NULL)",
        )
        db.exec("CREATE TABLE blocks (blk INTEGER PRIMARY KEY, data BLOB NOT NULL)")
        db.exec("INSERT INTO books (id, title, cat, seq, n) VALUES ('b1', '测试书目', '测试栏目', 1, 7)")
        // 6 篇正文命中 + 1 篇仅标题命中；正文按篇对齐拼成单块压缩写入
        val bodies = LinkedHashMap<Long, Pair<Int, ByteArray>>()
        var byteOff = 0
        fun put(id: Long, title: String, body: String) {
            val data = body.encodeToByteArray()
            bodies[id] = byteOff to data
            db.exec(
                "INSERT INTO arts (id, title, book, blk, off, nb) VALUES (?, ?, ?, 0, ?, ?)",
                id, title, "b1", byteOff, data.size,
            )
            byteOff += data.size
        }
        for (i in 1..6) put(i.toLong(), "篇目$i", "前文\n含有关键词的行$i\n后文")
        put(7L, "标题含关键词", "正文没有该词")
        val raw = ByteArray(bodies.values.sumOf { it.second.size })
        bodies.values.forEach { (off, data) -> data.copyInto(raw, off) }
        val def = Deflater()
        def.setInput(raw)
        def.finish()
        val comp = ByteArrayOutputStream()
        val buf = ByteArray(1 shl 16)
        while (!def.finished()) comp.write(buf, 0, def.deflate(buf))
        def.end()
        db.exec("INSERT INTO blocks (blk, data) VALUES (0, ?)", comp.toByteArray())
        queries = Queries(db)
    }

    @AfterTest
    fun teardown() {
        db.close()
        File(dbPath).delete()
    }

    @Test
    fun titleHitsRankFirst() {
        val all = queries.search("关键词", 10)
        assertEquals(7, all.size)
        assertEquals(7L, all[0].id) // 标题命中排最前
        assertEquals((1L..6L).toList(), all.drop(1).map { it.id })
    }

    @Test
    fun pagesConcatenateToFullSearch() {
        val all = queries.search("关键词", 10).map { it.id }
        val p0 = queries.searchPage("关键词", 0, 3).map { it.id }
        val p1 = queries.searchPage("关键词", 3, 3).map { it.id }
        val p2 = queries.searchPage("关键词", 6, 3).map { it.id }
        assertEquals(all, p0 + p1 + p2)
        assertEquals(listOf(7L, 1L, 2L), p0)
        assertEquals(listOf(3L, 4L, 5L), p1)
        assertEquals(listOf(6L), p2)
    }

    @Test
    fun offsetBeyondEndReturnsEmpty() {
        assertTrue(queries.searchPage("关键词", 100, 3).isEmpty())
    }

    @Test
    fun invalidArgsReturnEmpty() {
        assertTrue(queries.searchPage("关键词", 0, 0).isEmpty())
        assertTrue(queries.searchPage("关键词", -1, 3).isEmpty())
        assertTrue(queries.searchPage("", 0, 3).isEmpty())
    }

    @Test
    fun countHitsMatchesSearchSize() {
        assertEquals(7, queries.countHits("关键词"))
        assertEquals(0, queries.countHits("不存在的词"))
    }

    @Test
    fun snippetCarriesHitContext() {
        val hit = queries.searchPage("关键词", 0, 10).first { it.id == 1L }
        val snippet = hit.snippet
        assertEquals(true, snippet?.bodyHit)
        assertTrue(snippet?.lines?.any { it.contains("关键词") } == true)
    }

    @Test
    fun articleDetailDecodesBody() {
        val detail = queries.articleDetail(1L) ?: error("应为空非")
        assertEquals("前文\n含有关键词的行1\n后文", detail.body)
        assertEquals("测试书目", detail.bookTitle)
    }
}
