package org.xadfz.xianzhi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.ui.LocalSettings
import org.xadfz.xianzhi.ui.ThemeMode
import org.xadfz.xianzhi.util.ioDispatcher
import org.xadfz.xianzhi.util.platformName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val settings = LocalSettings.current!!

    val stats by produceState<Pair<Int, Int>?>(null) {
        value = withContext(ioDispatcher) {
            container.queries.totalBooks() to container.queries.totalArticles()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("设置") }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 字号（14–28sp，默认 16）
            SettingCard("正文字号") {
                Text("${settings.fontSize} sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.fontSize.toFloat(),
                    onValueChange = { settings.fontSize = it.toInt() },
                    valueRange = 14f..28f,
                    steps = 13,
                )
                Text(
                    "西安地方志",
                    fontSize = settings.fontSize.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // 行距
            SettingCard("行距") {
                Text("${(settings.lineSpacing * 10).toInt() / 10f} 倍行距", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.lineSpacing,
                    onValueChange = { settings.lineSpacing = it },
                    valueRange = 1.2f..2.0f,
                )
            }

            Spacer(Modifier.height(12.dp))

            // 主题
            SettingCard("主题") {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { settings.themeMode = ThemeMode.SYSTEM },
                        label = { Text("跟随系统") },
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { settings.themeMode = ThemeMode.LIGHT },
                        label = { Text("浅色") },
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { settings.themeMode = ThemeMode.DARK },
                        label = { Text("深色") },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 关于
            SettingCard("关于") {
                Text("西安地方志", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "版本 0.1.0 · $platformName" +
                        (stats?.let { " · ${it.first} 部书 / ${it.second} 篇" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("数据来源：西安市地方志办公室官网（xadfz.xa.gov.cn）「志鉴浏览」")
                Spacer(Modifier.height(8.dp))
                Text(
                    "版权声明：正文及配图著作权归西安市地方志办公室及各编纂单位所有，" +
                        "仅供个人学习研究使用，转载请注明出处。软件代码以 MIT 协议开源。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
