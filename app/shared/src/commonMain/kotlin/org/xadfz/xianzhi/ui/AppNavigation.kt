package org.xadfz.xianzhi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** 四个全局常驻目的地：书目章节 / 检索 / 收藏 / 设置（设置保持末位，符合使用直觉） */
enum class MainDest(val title: String, val icon: ImageVector, val cd: String) {
    BOOKS("书目", Icons.AutoMirrored.Filled.List, "选择书目章节"),
    SEARCH("检索", Icons.Filled.Search, "全文检索"),
    FAVORITES("收藏", Icons.Filled.Bookmark, "我的收藏"),
    SETTINGS("设置", Icons.Filled.Settings, "设置"),
}

private val IndicatorShape = RoundedCornerShape(percent = 50)

/**
 * 单个常驻入口：选中态为朱砂淡底胶囊 + 朱砂图标/文字，
 * 按压时轻微缩小并带平台涟漪反馈。
 */
@Composable
private fun NavItem(
    dest: MainDest,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f)
    val iconTint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val labelColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val indicator by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
    )

    Column(
        modifier = modifier
            .scale(scale)
            .semantics { contentDescription = dest.cd }
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(30.dp)
                .clip(IndicatorShape)
                .drawBehind { drawRect(indicator) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = dest.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = dest.title,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}

/**
 * 手机竖屏常驻底栏：任意页面层级（分类/书目/篇目/阅读/检索/设置）均可见。
 */
@Composable
fun MainNavBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hairline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = hairline,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, 1f),
                )
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(74.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainDest.entries.forEachIndexed { i, dest ->
                NavItem(
                    dest = dest,
                    selected = selectedIndex == i,
                    onClick = { onSelect(i) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 宽屏（平板/桌面大窗）常驻侧栏，与底栏入口、选中态、反馈完全一致。
 */
@Composable
fun MainNavRail(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hairline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier.drawBehind {
            drawRect(
                color = hairline,
                topLeft = androidx.compose.ui.geometry.Offset(size.width - 1f, 0f),
                size = androidx.compose.ui.geometry.Size(1f, size.height),
            )
        },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .width(92.dp)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MainDest.entries.forEachIndexed { i, dest ->
                NavItem(
                    dest = dest,
                    selected = selectedIndex == i,
                    onClick = { onSelect(i) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
