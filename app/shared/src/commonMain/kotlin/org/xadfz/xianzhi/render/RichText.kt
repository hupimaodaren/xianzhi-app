package org.xadfz.xianzhi.render

/**
 * 正文富文本解析。body 规则：
 * - 段落以 \n\n 分隔
 * - 图片位置为 ⟦img:文件名⟧ 标记（正则 ⟦img:([^⟧]+)⟧）
 *
 * 文字版：缺字图随文字流内嵌（约 1.2em）；内容配图标记已在库构建时移除。
 * 带图版：内容图块级撑满显示，可点击放大。
 */
sealed interface RichSegment {
    data class Text(val text: String) : RichSegment
    data class Image(val file: String) : RichSegment
}

private val IMG_MARKER = Regex("""⟦img:([^⟧]+)⟧""")

/** 将一段（段落内）切分为 文本/图片 序列 */
fun parseSegments(paragraph: String): List<RichSegment> {
    val result = mutableListOf<RichSegment>()
    var last = 0
    for (m in IMG_MARKER.findAll(paragraph)) {
        if (m.range.first > last) {
            result.add(RichSegment.Text(paragraph.substring(last, m.range.first)))
        }
        result.add(RichSegment.Image(m.groupValues[1]))
        last = m.range.last + 1
    }
    if (last < paragraph.length) {
        result.add(RichSegment.Text(paragraph.substring(last)))
    }
    return result
}

/** 正文按 \n\n 拆为非空段落 */
fun splitParagraphs(body: String): List<String> =
    body.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }

/** 整篇正文是否仅含一个图片标记（带图版块级图的常见形态） */
fun soleImage(paragraph: String): String? {
    val trimmed = paragraph.trim()
    val m = IMG_MARKER.matchEntire(trimmed) ?: return null
    return m.groupValues[1]
}

/** 去掉图片标记后的纯文本（检索定位用） */
fun plainText(raw: String): String = IMG_MARKER.replace(raw, "")
