package org.xadfz.xianzhi.ui

import androidx.compose.runtime.Composable

/** 系统返回键（Android 返回键；Desktop 暂由界面返回按钮处理） */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
