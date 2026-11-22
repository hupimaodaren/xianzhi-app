import java.io.File

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
            // 发布只面向 Windows（EXE 安装程序 + AppImage 目录）。jpackage 会校验目标格式与
            // 当前宿主平台是否匹配，故仅在 Windows 上启用；其他宿主只保留可本机运行的 Dmg/AppImage，
            // 便于在 macOS/Linux 开发机上直接 run 验证界面。
            val hostOs = System.getProperty("os.name").lowercase()
            targetFormats(
                *when {
                    hostOs.contains("windows") -> arrayOf(TargetFormat.Exe, TargetFormat.AppImage)
                    hostOs.contains("mac") -> arrayOf(TargetFormat.Dmg)
                    else -> arrayOf(TargetFormat.AppImage, TargetFormat.Deb)
                },
            )
            // -PwinEdition=image 打图文版（独立产品，可与文字版并存）；默认文字版
            // （macOS 只发布文字版 DMG，图文版体积过大且 mac 用户可用 Windows 版数据）
            val edition = (project.findProperty("winEdition") as String?) ?: "text"
            // 应用版本（需与 AppInfo.APP_VERSION、androidApp 的 versionName 一致）
            val appVersion = "0.6.0"
            packageName = if (edition == "image") "xianzhi-img" else "xianzhi"
            packageVersion = appVersion
            // 注意：jpackage 参数文件按平台 ANSI 编码读取，中文 vendor/menuGroup 会报
            // “Input length = 1”；打包元数据保持 ASCII（应用窗口标题仍为中文，不受影响）
            description = "Xian Local Chronicles"
            vendor = "Xian Place Names Association"
            // 内置数据（db/缺字图/配图）：打包前由发布脚本按版本放入 build/app-resources/data
            appResourcesRootDir.set(layout.buildDirectory.dir("app-resources"))
            // sqlite-jdbc 为非模块 jar，jdeps 无法自动识别其 JDK 模块依赖，显式补齐
            modules("java.sql", "java.naming", "java.management", "jdk.unsupported")
            // macOS 的 Dmg 打包不接受 0.x 版本号：jpackage 与 Compose 插件在校验 app-version 与
            // dmg 版本时都要求 MAJOR > 0（dmgPackageVersion 也不能写 0.6.0）。macOS 的 bundle 与
            // DMG 因此统一用 1.0.0，发布时由 CI 把 DMG 重命名为 xianzhi-<VER>-macos-<arch>.dmg。
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "org.xadfz.xianzhi"
                // 参考/工具类（Finder 中显示为「参考」分类）
                appCategory = "public.app-category.reference"
                minimumSystemVersion = "11.0"
                packageVersion = "1.0.0"
            }
            windows {
                dirChooser = true
                menuGroup = "Xianzhi"
                // EXE/安装包图标（运行时窗口标题栏图标在 Main.kt 用 resources/icon.png 设置）
                iconFile.set(project.file("icons/icon.ico"))
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
// 开发期界面截图：离屏渲染各页面为 PNG（无窗口环境可用），产物 build/screenshots
// 注意：shared 里的 sqlite-jdbc 已被裁剪为仅含 Windows/macOS 原生库（发布目标只有这两个平台），
// 故这里额外挂一份完整 jar：Linux 开发机靠它加载本机原生库，Windows/macOS 上只是重复挂载，
// 不影响运行。
val sqliteFullJar = configurations.detachedConfiguration(
    dependencies.create(libs.sqlite.jdbc.get().let { "${it.module}:${it.versionConstraint}" }),
)
val screenshotTask = tasks.register<JavaExec>("screenshot") {
    group = "verification"
    description = "离屏渲染界面截图到 build/screenshots"
    mainClass.set("org.xadfz.xianzhi.desktop.ScreenshotKt")
    // skiko 会把原生库解包到 -Dskiko.cache.dir（及 java.io.tmpdir），目录必须先存在
    doFirst { File(skikoCachePath).mkdirs() }
    classpath = sourceSets["main"].runtimeClasspath + files(sqliteFullJar)
    dependsOn(tasks.named("classes"))
    jvmArgs(
        "-Dskiko.cache.dir=$skikoCachePath",
        "-Dxianzhi.home=$runDataPath",
        "-Dsun.java2d.uiScale=1.0",
        // skiko 找不到预置原生库时会把 so/dylib 解包到 skiko.data.path（默认 ~/.skiko，
        // 沙箱下主目录不可写，mkdirs 失败后 createTempFile 报 No such file or directory）。
        "-Dskiko.data.path=" + layout.buildDirectory.dir("skiko").get().asFile.absolutePath,
        "-Djava.io.tmpdir=" + layout.buildDirectory.dir("tmp").get().asFile.absolutePath,
    )
    // 截图参数透传：Gradle 不会把 -P 项目属性或 -D 系统属性自动带进渲染进程。
    // 支持 gradle :desktopApp:screenshot -Pshots.theme=dark -Pshots.only=reader，
    // 其中 shots.out 缺省为 build/screenshots，shots.width/height 缺省 1264x792。
    val defaultOut = layout.buildDirectory.dir("screenshots").get().asFile.absolutePath
    val shotsArgs = buildMap {
        project.properties.forEach { (k, v) ->
            val key = k.toString()
            if (key.startsWith("shots.") && v != null) put(key, v.toString())
        }
        System.getProperties().forEach { (k, v) ->
            val key = k.toString()
            if (key.startsWith("shots.")) put(key, v.toString())
        }
        putIfAbsent("shots.out", defaultOut)
    }
    doFirst {
        shotsArgs.forEach { (key, value) -> systemProperty(key, value) }
    }
}
