package org.xadfz.xianzhi.db

import org.xadfz.xianzhi.db.openWritableDatabase
import java.io.File
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
        db.exec("CREATE TABLE books (id TEXT PRIMARY KEY, title TEXT, cat TEXT, seq INTEGER, n INTEGER)")
        db.exec(
            "CREATE TABLE arts (" +
                "id INTEGER PRIMARY KEY, title TEXT, vol TEXT, date TEXT, path TEXT, book TEXT, body TEXT)",
        )
        db.exec("INSERT INTO books (id, title, cat, seq, n) VALUES ('b1', '测试书目', '测试栏目', 1, 7)")
        // 6 篇正文命中 + 1 篇仅标题命中
        for (i in 1..6) {
            db.exec(
                "INSERT INTO arts (id, title, book, body) VALUES (?, ?, ?, ?)",
                i, "篇目$i", "b1", "前文\n含有关键词的行$i\n后文",
            )
        }
        db.exec(
            "INSERT INTO arts (id, title, book, body) VALUES (?, ?, ?, ?)",
            7, "标题含关键词", "b1", "正文没有该词",
        )
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
}
