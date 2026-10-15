package org.xadfz.xianzhi.history

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryRepositoryTest {

    private lateinit var dbPath: String
    private lateinit var repo: HistoryRepository

    @BeforeTest
    fun setup() {
        val tmp = java.io.File(System.getProperty("java.io.tmpdir"), "xz_his_test_${System.nanoTime()}.db")
        dbPath = tmp.absolutePath
        repo = HistoryRepository(dbPath)
    }

    @AfterTest
    fun teardown() {
        repo.close()
        java.io.File(dbPath).delete()
    }

    @Test
    fun recordThenListFields() {
        repo.record(1L, "篇目甲", "b1", "书名甲")
        val list = repo.list()
        assertEquals(1, list.size)
        val e = list[0]
        assertEquals(1L, e.articleId)
        assertEquals("篇目甲", e.title)
        assertEquals("b1", e.bookId)
        assertEquals("书名甲", e.bookTitle)
        assertTrue(e.lastReadAt > 0)
        assertEquals(1, repo.count())
    }

    @Test
    fun rereadBumpsToTopAndDedupes() {
        repo.record(1L, "甲", "b", "书")
        repo.record(2L, "乙", "b", "书")
        repo.record(3L, "丙", "b", "书")
        // 重读 1：总数不变、置顶、标题更新
        repo.record(1L, "甲·改", "b", "书")
        val list = repo.list()
        assertEquals(3, list.size)
        assertEquals(listOf(1L, 3L, 2L), list.map { it.articleId })
        assertEquals("甲·改", list[0].title)
    }

    @Test
    fun capEvictsOldest() {
        for (i in 1L..(HistoryRepository.MAX_ENTRIES + 5L)) {
            repo.record(i, "t$i", "b", "书")
        }
        assertEquals(HistoryRepository.MAX_ENTRIES, repo.count())
        val list = repo.list()
        assertEquals(HistoryRepository.MAX_ENTRIES, list.size)
        // 最旧的 5 条（id 1..5）被淘汰，最新写入的在最前
        assertEquals((HistoryRepository.MAX_ENTRIES + 5).toLong(), list.first().articleId)
        assertEquals(6L, list.last().articleId)
        assertNull(list.firstOrNull { it.articleId <= 5L })
    }

    @Test
    fun removeAndClear() {
        repo.record(1L, "甲", "b", "书")
        repo.record(2L, "乙", "b", "书")
        repo.remove(1L)
        assertEquals(1, repo.count())
        assertEquals(2L, repo.list()[0].articleId)
        repo.clear()
        assertEquals(0, repo.count())
        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun persistsAcrossReopen() {
        repo.record(7L, "七", "b7", "书七")
        repo.close()
        val repo2 = HistoryRepository(dbPath)
        try {
            assertEquals(1, repo2.count())
            assertEquals("七", repo2.list()[0].title)
        } finally {
            repo2.close()
        }
        // teardown 里的 repo.close() 对已关闭连接重复调用无害（sqlite-jdbc close 幂等）
        repo = repo2
    }

    @Test
    fun versionBumpsOnWrites() {
        val v0 = repo.version
        repo.record(1L, "t", "b", "bt")
        assertTrue(repo.version > v0)
    }
}
