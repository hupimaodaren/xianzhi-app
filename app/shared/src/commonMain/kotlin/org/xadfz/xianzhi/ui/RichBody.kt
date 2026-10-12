package org.xadfz.xianzhi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.render.RichSegment
import org.xadfz.xianzhi.render.parseSegments
import org.xadfz.xianzhi.render.soleImage
import org.xadfz.xianzhi.render.splitParagraphs

/**
 * 正文渲染：段落 + ⟦img:⟧ 标记图文混排。
 * - 缺字图：随文字流内嵌，高度约 1.2em，垂直居中
 * - 内容图（带图版）：块级撑满，点击看大图
 * - 检索跳入：keyword 黄底高亮
 */
@Composable
fun RichBody(
    body: String,
    container: AppContainer,
    fontSize: Int,
    lineSpacing: Float,
    keyword: String? = null,
) {
    val paragraphs = remember(body) { splitParagraphs(body) }
    Column(modifier = Modifier.fillMaxWidth()) {
        for (p in paragraphs) {
            val imageOnly = soleImage(p)
            if (imageOnly != null) {
                BlockImage(imageOnly, container)
            } else {
                InlineParagraph(p, container, fontSize, lineSpacing, keyword)
            }
        }
    }
}

@Composable
private fun InlineParagraph(
    paragraph: String,
    container: AppContainer,
    fontSize: Int,
    lineSpacing: Float,
    keyword: String?,
) {
    val segments = remember(paragraph) { parseSegments(paragraph) }
    // 先判定哪些标记对应缺字图（文件存在），builder 内不能调用 remember
    val missingFiles = remember(paragraph) {
        segments.mapNotNull { (it as? RichSegment.Image)?.file }
            .filter { org.xadfz.xianzhi.util.fileExists(container.missingImagePath(it)) }
            .toSet()
    }
    val inlineContents = HashMap<String, InlineTextContent>()
    val annotated = buildAnnotatedString {
        var imgIndex = 0
        for (seg in segments) {
            when (seg) {
                is RichSegment.Text -> appendHighlighted(seg.text, keyword)
                is RichSegment.Image -> {
                    if (seg.file in missingFiles) {
                        val id = "img$imgIndex"
                        imgIndex++
                        appendInlineContent(id, "[字]")
                        inlineContents[id] = InlineTextContent(
                            Placeholder(1.2.em, 1.2.em, PlaceholderVerticalAlign.Center),
                        ) {
                            val painter = rememberFilePainter(container.missingImagePath(seg.file))
                            if (painter != null) {
                                Image(
                                    painter = painter,
                                    contentDescription = "缺字",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((fontSize * 1.2f).dp),
                                )
                            }
                        }
                    } else if (container.isImageEdition) {
                        // 带图版行内配图（非整段图）：占位提示
                        append("［配图］")
                    }
                    // 文字版非缺字图标记：库构建时已移除，此处兜底不渲染
                }
            }
        }
    }
    Text(
        text = annotated,
        inlineContent = inlineContents,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * lineSpacing).sp,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun BlockImage(file: String, container: AppContainer) {
    val path = if (
        org.xadfz.xianzhi.util.fileExists(container.missingImagePath(file))
    ) {
        container.missingImagePath(file)
    } else {
        container.contentImagePath(file)
    }
    if (path == null) return // 文字版，内容配图标记已在库中移除
    val painter = rememberFilePainter(path) ?: return
    var zoomed by remember { mutableStateOf(false) }
    Image(
        painter = painter,
        contentDescription = "配图",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { zoomed = true },
    )
    if (zoomed) {
        Dialog(onDismissRequest = { zoomed = false }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clickable { zoomed = false },
            ) {
                Image(painter = painter, contentDescription = "配图大图", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** 追加文本并对关键词黄底高亮 */
private fun AnnotatedString.Builder.appendHighlighted(text: String, keyword: String?) {
    if (keyword.isNullOrBlank()) {
        append(text)
        return
    }
    var start = 0
    while (true) {
        val idx = text.indexOf(keyword, start)
        if (idx < 0) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, idx))
        withStyle(SpanStyle(background = Color(0xFFFFEB3B))) {
            append(text.substring(idx, idx + keyword.length))
        }
        start = idx + keyword.length
    }
}
