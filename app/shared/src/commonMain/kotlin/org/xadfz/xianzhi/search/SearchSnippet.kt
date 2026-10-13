package org.xadfz.xianzhi.search

/**
 * 全文检索结果的上下文片段。
 *
 * @param lines 命中原文片段（已去图片标记、去空行），含命中行及其前后各 [CONTEXT_LINES] 行
 * @param hitLine 关键词所在行在 [lines] 中的下标；-1 表示仅标题命中、正文无关键词
 * @param truncatedBefore / truncatedAfter 片段前后是否还有未展示的行（界面用省略号提示）
 */
data class SearchSnippet(
    val lines: List<String>,
    val hitLine: Int,
    val truncatedBefore: Boolean,
    val truncatedAfter: Boolean,
) {
    val bodyHit: Boolean get() = hitLine >= 0

    companion object {
        /** 命中位置前后各取 2 行 */
        const val CONTEXT_LINES = 2
        private val IMG_MARKER = Regex("""⟦img:[^⟧]+⟧""")
        private val BLANK = Regex("""\s+""")

        /** 去掉图片标记与多余空白，得到纯文本行 */
        fun cleanLine(raw: String): String =
            BLANK.replace(IMG_MARKER.replace(raw, ""), " ").trim()

        fun build(body: String, keyword: String): SearchSnippet? {
            val lines = body.split('\n').map { cleanLine(it) }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return null
            if (keyword.isBlank()) {
                val head = lines.take(CONTEXT_LINES + 1)
                return SearchSnippet(head, -1, false, lines.size > head.size)
            }
            val hit = lines.indexOfFirst { it.contains(keyword) }
            if (hit < 0) {
                // 仅标题命中：取正文开头数行作预览
                val head = lines.take(CONTEXT_LINES + 1)
                return SearchSnippet(head, -1, false, lines.size > head.size)
            }
            val from = (hit - CONTEXT_LINES).coerceAtLeast(0)
            val to = (hit + CONTEXT_LINES).coerceAtMost(lines.lastIndex)
            return SearchSnippet(
                lines = lines.subList(from, to + 1),
                hitLine = hit - from,
                truncatedBefore = from > 0,
                truncatedAfter = to < lines.lastIndex,
            )
        }
    }
}
