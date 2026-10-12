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
            // msi 安装包；绿色 zip 由 packageDistributionForCurrentOS 任务自动产出
            targetFormats(TargetFormat.Msi)
            packageName = "xianzhi"
            packageVersion = "0.1.0"
            description = "西安地方志"
            vendor = "西安地名协会"
            windows {
                dirChooser = true
                menuGroup = "西安地方志"
                // 无代码签名：msi 首次运行会有 SmartScreen 提示，过渡期可用 zip 绿色版
            }
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
