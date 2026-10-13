package org.xadfz.xianzhi.favorites

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FavoritesRepositoryTest {

    private lateinit var dbPath: String
    private lateinit var repo: FavoritesRepository

    @BeforeTest
    fun setup() {
        val tmp = java.io.File(System.getProperty("java.io.tmpdir"), "xz_fav_test_${System.nanoTime()}.db")
        dbPath = tmp.absolutePath
        repo = FavoritesRepository(dbPath)
    }

    @AfterTest
    fun teardown() {
        repo.close()
        java.io.File(dbPath).delete()
    }

    @Test
    fun toggleFavorite_addsAndRemoves() {
        assertFalse(repo.isFavorited(1L))
        val now = repo.toggleFavorite(1L, "篇目甲", "b1", "书名甲", "栏目甲")
        assertTrue(now)
        assertTrue(repo.isFavorited(1L))

        val fav = repo.getFavoriteByArticleId(1L)
        assertNotNull(fav)
        assertEquals("篇目甲", fav.title)
        assertEquals("b1", fav.bookId)
        assertEquals("书名甲", fav.bookTitle)
        assertNull(fav.collectionId)

        val again = repo.toggleFavorite(1L, "篇目甲", "b1", "书名甲", "栏目甲")
        assertFalse(again)
        assertFalse(repo.isFavorited(1L))
        assertNull(repo.getFavoriteByArticleId(1L))
    }

    @Test
    fun createAndListCollections() {
        assertNotNull(repo.createCollection("合集一"))
        assertNotNull(repo.createCollection("合集二"))
        assertNull(repo.createCollection("合集一")) // 重名

        val colls = repo.listCollections()
        assertEquals(2, colls.size)
        assertEquals("合集一", colls[0].name)
    }

    @Test
    fun moveFavoriteBetweenCollections() {
        assertNotNull(repo.createCollection("甲"))
        assertNotNull(repo.createCollection("乙"))
        val colls = repo.listCollections()
        val idA = colls.first { it.name == "甲" }.id
        val idB = colls.first { it.name == "乙" }.id

        repo.addFavorite(10L, "t", "b", "bt", "c", collectionId = null)
        assertNull(repo.getFavoriteByArticleId(10L)?.collectionId)

        repo.moveToCollection(10L, idA)
        assertEquals(idA, repo.getFavoriteByArticleId(10L)?.collectionId)

        repo.moveToCollection(10L, idB)
        assertEquals(idB, repo.getFavoriteByArticleId(10L)?.collectionId)

        repo.moveToCollection(10L, null)
        assertNull(repo.getFavoriteByArticleId(10L)?.collectionId)
    }

    @Test
    fun listFavoritesFiltersByCollection() {
        assertNotNull(repo.createCollection("A"))
        val idA = repo.listCollections().first().id
        repo.addFavorite(1L, "t1", "b", "bt", "c", idA)
        repo.addFavorite(2L, "t2", "b", "bt", "c", null)

        val all = repo.listFavorites(null)
        assertEquals(2, all.size)

        val inA = repo.listFavorites(idA)
        assertEquals(1, inA.size)
        assertEquals("t1", inA[0].title)

        val ungrouped = repo.listFavorites(-1L)
        assertEquals(1, ungrouped.size)
        assertEquals("t2", ungrouped[0].title)
    }

    @Test
    fun deleteCollectionMovesFavoritesToUngrouped() {
        assertNotNull(repo.createCollection("X"))
        val idX = repo.listCollections().first().id
        repo.addFavorite(5L, "t", "b", "bt", "c", idX)
        assertEquals(idX, repo.getFavoriteByArticleId(5L)?.collectionId)

        repo.deleteCollection(idX)
        assertEquals(0, repo.listCollections().size)
        // ON DELETE SET NULL：收藏仍在，但合集变为未分组
        assertNull(repo.getFavoriteByArticleId(5L)?.collectionId)
    }

    @Test
    fun renameCollection() {
        assertNotNull(repo.createCollection("旧名"))
        val id = repo.listCollections().first().id
        assertTrue(repo.renameCollection(id, "新名"))
        assertEquals("新名", repo.listCollections().first().name)
    }

    @Test
    fun versionBumpsOnWrites() {
        val v0 = repo.version
        repo.toggleFavorite(1L, "t", "b", "bt", "c")
        assertTrue(repo.version > v0)
    }
}
