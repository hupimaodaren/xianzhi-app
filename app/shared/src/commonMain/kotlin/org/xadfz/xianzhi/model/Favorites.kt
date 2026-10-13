package org.xadfz.xianzhi.model

/** 收藏合集（用户自定义分组） */
data class Collection(
    val id: Long,
    val name: String,
    val sort: Int,
)

/** 单条收藏（一篇志书篇目） */
data class Favorite(
    val id: Long,
    val articleId: Long,
    val title: String,
    val bookId: String,
    val bookTitle: String,
    val cat: String,
    /** 所属合集 id；null 表示「未分组」 */
    val collectionId: Long?,
    val createdAt: Long,
)
