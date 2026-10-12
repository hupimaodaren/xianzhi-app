package org.xadfz.xianzhi.ui

import androidx.compose.runtime.Composable

/** Desktop 返回由各页面 TopBar 返回按钮处理；系统返回键为 Android 专有。 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
