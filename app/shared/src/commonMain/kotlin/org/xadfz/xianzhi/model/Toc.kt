package org.xadfz.xianzhi.model

/**
 * 一部书的多级目录树。
 *
 * 数据源 `arts.path` 是 "/" 分隔的祖先路径（0~4 级），且**同一父节点下子层级深度不齐**
 * （中间层级在源目录中缺失，例如某分志下直接挂 4 级节点）。因此按「路径前缀逐级归组」
 * 建树，不假设等深；书根文章（path 为空）直接列在根部。
 *
 * 同级顺序取 `articles` 的原有位次（库内按 arts.id 排序，即原始志书顺序）：
 * 分组的位次是它首次出现时的下标，文章的位次是它自身的下标，两者交错排列。
 */

sealed interface TocNode {
    val key: String
    val title: String
    /** 缩进层级，从 1 开始 */
    val level: Int
}

/** 目录分组节点（层级标题），可展开/收起 */
data class TocGroup(
    override val key: String,
    override val title: String,
    override val level: Int,
    val children: List<TocNode>,
    /** 该层级本身也是一篇文章（数据源中其 title 等于 path 末级）时的文章 id */
    val articleId: Long? = null,
    /** 子孙篇目数 */
    val leafCount: Int = 0,
) : TocNode

/** 篇目叶子 */
data class TocLeaf(
    val article: Article,
) : TocNode {
    override val key: String get() = "a-${article.id}"
    override val title: String get() = article.title
    override val level: Int get() = article.depth + 1
}

/** 建树过程中的可变节点 */
private class Node(
    val key: String,
    val title: String,
    val level: Int,
    /** 子节点（子分组 [Node] 或子文章 [Article]）及其在 articles 中的位次 */
    val childOrder: MutableList<Pair<Int, Any>> = mutableListOf(),
    /** 该节点自身也是一篇文章时的文章 */
    var selfArticle: Article? = null,
)

private fun Node.frozen(): TocGroup {
    val children = childOrder.sortedBy { it.first }.flatMap { (_, c) ->
        when (c) {
            is Node -> listOf(c.frozen())
            is Article -> listOf(TocLeaf(c))
            else -> emptyList()
        }
    }
    // 子孙篇目数：子分组递归其自身计数，子文章计 1；该层级自身文章的「读本篇」也计入
    val leafCount = children.sumOf { if (it is TocGroup) it.leafCount else 1 } +
        if (selfArticle != null) 1 else 0
    return TocGroup(
        key = key,
        title = title,
        level = level,
        children = children,
        articleId = selfArticle?.id,
        leafCount = leafCount,
    )
}

private fun segments(path: String): List<String> =
    path.split('/').filter { it.isNotBlank() }

fun buildToc(articles: List<Article>): List<TocNode> {
    val root = Node("", "", 0)
    val groups = LinkedHashMap<String, Node>()

    fun groupFor(key: String, pos: Int): Node {
        groups[key]?.let { return it }
        val n = Node(key, key.substringAfterLast('/'), key.count { it == '/' } + 1)
        groups[key] = n
        val parentKey = key.substringBeforeLast('/', "")
        if (parentKey.isEmpty()) root.childOrder.add(pos to n)
        else groupFor(parentKey, pos).childOrder.add(pos to n)
        return n
    }

    // 第一遍：只建分组骨架，避免文章先挂成叶子后又被同名分组替代导致重复。
    // 分组位次取它首次出现时的 articles 下标。
    articles.forEachIndexed { i, a ->
        var key = ""
        for (s in segments(a.path)) {
            key = if (key.isEmpty()) s else "$key/$s"
            groupFor(key, i)
        }
    }
    // 第二遍：文章挂到 path 末级分组下。全库仅一例「节点既当分组又当文章」
    // （其 title 等于 path 末级），该情形由分组标题右侧的「读本篇」入口承载。
    articles.forEachIndexed { i, a ->
        val segs = segments(a.path)
        if (segs.isEmpty()) {
            root.childOrder.add(i to a)
            return@forEachIndexed
        }
        var key = ""
        for (s in segs) key = if (key.isEmpty()) s else "$key/$s"
        val parent = groups.getValue(key)
        val last = segs.last()
        if (a.title == last && parent.selfArticle == null) parent.selfArticle = a
        else parent.childOrder.add(i to a)
    }
    return root.childOrder.sortedBy { it.first }.flatMap { (_, c) ->
        when (c) {
            is Node -> listOf(c.frozen())
            is Article -> listOf(TocLeaf(c))
            else -> emptyList()
        }
    }
}

/** 默认展开根级分组，深层级按需展开（深层级动辄数百篇，全展开会淹没根部） */
fun defaultExpanded(nodes: List<TocNode>): Set<String> =
    nodes.filterIsInstance<TocGroup>().map { it.key }.toSet()

/** 按展开状态把树拍平成列表，供 LazyColumn 直接消费 */
fun flattenToc(nodes: List<TocNode>, expanded: Set<String>): List<TocNode> {
    val out = mutableListOf<TocNode>()
    fun walk(list: List<TocNode>) {
        for (n in list) {
            out.add(n)
            if (n is TocGroup && n.key in expanded) walk(n.children)
        }
    }
    walk(nodes)
    return out
}

/** 全部层级分组 key（「全部展开」用；层级数无上限） */
fun allGroupKeys(nodes: List<TocNode>): Set<String> =
    nodes.filterIsInstance<TocGroup>().flatMapTo(mutableSetOf()) { g ->
        sequence {
            yield(g.key)
            yieldAll(allGroupKeys(g.children))
        }.asIterable()
    }

/**
 * 篇目展开状态的进程内记忆：同一部书在层级间来回（进正文再返回、切 Tab）时
 * 保留用户的展开选择；进程重启恢复默认（仅根级展开）。
 */
object TocExpansionMemory {
    private val saved = mutableMapOf<String, Set<String>>()

    fun load(bookId: String, nodes: List<TocNode>): Set<String> =
        saved[bookId] ?: defaultExpanded(nodes)

    fun save(bookId: String, keys: Set<String>) {
        saved[bookId] = keys
    }
}
