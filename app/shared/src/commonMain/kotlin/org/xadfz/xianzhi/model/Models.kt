package org.xadfz.xianzhi.model

/** 栏目下的一部书（books 表） */
data class Book(
    val id: String,
    val title: String,
    val cat: String,
    val n: Int,
)

/** 首页栏目（聚合 books 行） */
data class Category(
    val name: String,
    val books: List<Book>,
) {
    val articleCount: Int get() = books.sumOf { it.n }
}

/** 篇目列表项（不含正文，查询快） */
data class Article(
    val id: Long,
    val title: String,
    val vol: String,
    val date: String,
)

/** 篇目详情（含正文） */
data class ArticleDetail(
    val id: Long,
    val bookId: String,
    val bookTitle: String,
    val title: String,
    val vol: String,
    val date: String,
    val body: String,
)

/** 上一篇/下一篇 */
data class Neighbor(
    val id: Long,
    val title: String,
)

/** 全文检索命中 */
data class SearchHit(
    val id: Long,
    val title: String,
    val book: String,
    val bookTitle: String,
    val cat: String,
)
