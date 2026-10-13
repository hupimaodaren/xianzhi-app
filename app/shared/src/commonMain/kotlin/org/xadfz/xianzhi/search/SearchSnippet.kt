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

        /** 命中行超过该长度时，围绕关键词截取窗口，保证关键词在片段中居中可见 */
        const val LINE_WINDOW = 50

        /** 窗口内关键词前后各保留的字符数 */
        const val WINDOW_SIDE = 20

        private val IMG_MARKER = Regex("""⟦img:[^⟧]+⟧""")
        private val BLANK = Regex("""\s+""")

        /** 去掉图片标记与多余空白，得到纯文本行 */
        fun cleanLine(raw: String): String =
            BLANK.replace(IMG_MARKER.replace(raw, ""), " ").trim()

        /**
         * 超长命中行围绕关键词截取窗口：关键词前 [WINDOW_SIDE] 字、后 [WINDOW_SIDE] 字，
         * 截断处补「…」。短行原样返回。
         */
        fun windowAroundKeyword(line: String, keyword: String): String {
            if (line.length <= LINE_WINDOW) return line
            val idx = line.indexOf(keyword).coerceAtLeast(0)
            val from = (idx - WINDOW_SIDE).coerceAtLeast(0)
            val to = (idx + keyword.length + WINDOW_SIDE).coerceAtMost(line.length)
            val head = if (from > 0) "…" else ""
            val tail = if (to < line.length) "…" else ""
            return head + line.substring(from, to) + tail
        }

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
                lines = lines.subList(from, to + 1)
                    .mapIndexed { i, line ->
                        // 仅命中行做关键词居中窗口，上下文行保持原样
                        if (from + i == hit) windowAroundKeyword(line, keyword) else line
                    },
                hitLine = hit - from,
                truncatedBefore = from > 0,
                truncatedAfter = to < lines.lastIndex,
            )
        }
    }
}
