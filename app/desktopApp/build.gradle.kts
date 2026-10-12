import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.material3)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.core)
}

compose.desktop {
    application {
        mainClass = "org.xadfz.xianzhi.desktop.MainKt"

        nativeDistributions {
            // EXE 安装程序（Inno Setup，dirChooser 允许自选安装路径）+ AppImage 目录
            // （便携免安装 zip 由发布脚本对 app-image 目录直接打包）
            targetFormats(TargetFormat.Exe, TargetFormat.AppImage)
            // -PwinEdition=image 打图文版（独立产品，可与文字版并存）；默认文字版
            val edition = (project.findProperty("winEdition") as String?) ?: "text"
            packageName = if (edition == "image") "xianzhi-img" else "xianzhi"
            packageVersion = "0.1.0"
            // 注意：jpackage 参数文件按平台 ANSI 编码读取，中文 vendor/menuGroup 会报
            // “Input length = 1”；打包元数据保持 ASCII（应用窗口标题仍为中文，不受影响）
            description = "Xian Local Chronicles"
            vendor = "Xian Place Names Association"
            // 内置数据（db/缺字图/配图）：打包前由发布脚本按版本放入 build/app-resources/data
            appResourcesRootDir.set(layout.buildDirectory.dir("app-resources"))
            // sqlite-jdbc 为非模块 jar，jdeps 无法自动识别其 JDK 模块依赖，显式补齐
            modules("java.sql", "java.naming", "java.management", "jdk.unsupported")
            windows {
                dirChooser = true
                menuGroup = "Xianzhi"
                // 无代码签名：首次运行会有 SmartScreen 提示
            }
        }
        buildTypes.release {
            // 桌面本地分发，无需 ProGuard 混淆裁剪；关闭以保证 sqlite-jdbc/skiko 反射正常
            proguard { isEnabled = false }
        }
    }
}

// 开发期 run：把 Skiko 原生库缓存与应用数据目录放到项目 build/ 内，
// 避免依赖用户主目录权限（不影响打包后的正式分发包，其运行时仍使用默认位置）。
// 注意：Compose 插件在 afterEvaluate 会重置 run 任务的 jvmArgs，
// 必须在 doFirst（执行前最后时机）追加才不会被覆盖。
val skikoCachePath = layout.buildDirectory.dir("skiko").get().asFile.absolutePath.replace('\\', '/')
val runDataPath = layout.buildDirectory.dir("run-data").get().asFile.absolutePath.replace('\\', '/')
tasks.matching { it.name == "run" }.configureEach {
    doFirst {
        // jvmArgs(...) 为追加语义；doFirst 晚于插件的 afterEvaluate 重置，故不会被覆盖
        (this as? JavaExec)?.jvmArgs(
            "-Dskiko.cache.dir=$skikoCachePath",
            "-Dxianzhi.home=$runDataPath",
            // 开发机若使用非 JBR 的通用 JDK 且系统缩放为 125%，AWT 窗口尺寸不会按 DPI 放大，
            // 导致 Compose 内容（按 density 放大）溢出窗口。开发期固定 UI 缩放为 1 以便验证；
            // 正式打包随 Compose 使用 JBR，本身 HiDPI 处理正确，不受此参数影响。
            "-Dsun.java2d.uiScale=1.0",
        )
    }
}
