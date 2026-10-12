package org.xadfz.xianzhi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withContext
import org.xadfz.xianzhi.data.AndroidDataDeployer
import org.xadfz.xianzhi.data.AppContainer
import org.xadfz.xianzhi.settings.AndroidApp
import org.xadfz.xianzhi.ui.XianzhiApp
import org.xadfz.xianzhi.util.ioDispatcher

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidApp.init(applicationContext)

        setContent {
            var container by remember { mutableStateOf<AppContainer?>(null) }

            LaunchedEffect(Unit) {
                // 首启：从 assets 流式复制 db（174MB）与图片到 filesDir
                container = withContext(ioDispatcher) { AndroidDataDeployer.ensureData() }
            }

            val ready = container
            if (ready == null) {
                DeploySplash()
            } else {
                XianzhiApp(ready)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DeploySplash() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("西安地方志", fontSize = 22.sp)
                LinearProgressIndicator(
                    progress = { if (AndroidDataDeployer.progress < 0) 0f else AndroidDataDeployer.progress },
                    modifier = Modifier.width(220.dp).padding(top = 16.dp),
                )
                Text(
                    AndroidDataDeployer.message,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
