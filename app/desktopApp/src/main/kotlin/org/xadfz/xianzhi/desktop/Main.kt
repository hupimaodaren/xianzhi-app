package org.xadfz.xianzhi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.data.DesktopDataDeployer
import org.xadfz.xianzhi.ui.XianzhiApp

fun main() = application {
    var container by remember { mutableStateOf<AppContainer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { DesktopDataDeployer.ensureData() }
        }.onSuccess { container = it }
            .onFailure { error = it.message ?: it.toString() }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "西安地方志",
        state = rememberWindowState(size = DpSize(1024.dp, 768.dp)),
    ) {
        val ready = container
        if (ready != null) {
            XianzhiApp(ready)
        } else {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (error == null) {
                            Text("西安地方志", fontSize = 22.sp)
                            CircularProgressIndicator(Modifier.padding(top = 16.dp))
                            Text("正在准备数据…", fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                        } else {
                            Text("数据未就绪", fontSize = 20.sp)
                            Text(
                                error!!,
                                fontSize = 13.sp,
                                modifier = Modifier.width(480.dp).padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
