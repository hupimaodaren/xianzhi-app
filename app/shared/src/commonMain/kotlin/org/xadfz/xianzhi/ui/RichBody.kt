package org.xadfz.xianzhi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.render.RichSegment
import org.xadfz.xianzhi.render.parseSegments
import org.xadfz.xianzhi.render.soleImage

/**
 * 单个段落（或块级配图）的渲染。阅读页把每个段落作为独立的懒列表项，
 * 以便按段落滚动定位（检索跳入居中）。
 */
@Composable
fun ParagraphBlock(
    paragraph: String,
    container: AppContainer,
    fontSize: Int,
    lineSpacing: Float,
    keyword: String?,
) {
    val imageOnly = soleImage(paragraph)
    if (imageOnly != null) {
        BlockImage(imageOnly, container)
    } else {
        InlineParagraph(paragraph, container, fontSize, lineSpacing, keyword)
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
    val missingFiles = remember(paragraph, container) {
        segments.mapNotNull { (it as? RichSegment.Image)?.file }
            .filter { org.xadfz.xianzhi.util.fileExists(container.missingImagePath(it)) }
            .toSet()
    }
    // 注解文本与内联内容表成对缓存：仅当段落内容/字号/高亮词变化（如调节字号）才
    // 重新解析与查文件，其余重组（滚动、父级状态变化）直接复用
    val (annotated, inlineContents) = remember(paragraph, keyword, fontSize, container, missingFiles) {
        val contents = HashMap<String, InlineTextContent>()
        val text = buildAnnotatedString {
            var imgIndex = 0
            for (seg in segments) {
                when (seg) {
                    is RichSegment.Text -> appendHighlighted(seg.text, keyword)
                    is RichSegment.Image -> {
                        if (seg.file in missingFiles) {
                            val id = "img$imgIndex"
                            imgIndex++
                            appendInlineContent(id, "[字]")
                            contents[id] = InlineTextContent(
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
        text to contents
    }
    // 中文正文排版：段首空两字（仅对成句的正文段落，短行如目录式条目保持齐头），
    // 段间留白略大于行距，使整页呈「行内紧密、段间舒展」的典籍观感
    // 段首缩进两字：直接用 sp 而非 em —— em 依赖样式里的字号换算像素，
    // 而文本样式在段落构建器中可能尚未解析出字号，会抛 "Only Sp can convert to Px"
    val indent = if (paragraph.length >= 20) TextIndent(firstLine = (fontSize * 2).sp) else TextIndent.None
    // 字号、行距、颜色必须与 textIndent 一起放进 TextStyle：
    // 若 style 里字号留空（Unspecified）而段首缩进用 em，段落构建器无法换算成像素
    Text(
        text = annotated,
        inlineContent = inlineContents,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * lineSpacing).sp,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface,
            textIndent = indent,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
            .padding(vertical = 10.dp)
            .clip(MaterialTheme.shapes.small)
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
        // 藤黄纸色底纹替代刺眼纯黄，与宣纸基调同属暖调，长段高亮不炫目
        withStyle(SpanStyle(background = Color(0xFFF2D9A0))) {
            append(text.substring(idx, idx + keyword.length))
        }
        start = idx + keyword.length
    }
}
