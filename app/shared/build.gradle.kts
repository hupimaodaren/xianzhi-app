import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// 从缓存中的 sqlite-jdbc 原始 jar 裁剪出只保留 Java 类、services 与桌面端原生库的运行时 jar
// （Android 用系统 SQLite，不需要任何原生库；桌面端只发布 Windows 与 macOS(Intel/Apple Silicon)，
//  Linux / FreeBSD / Linux-Musl 等原生库约 18.35MB 全部剔除）
val sqliteDesktopOnlyJar = tasks.register("sqliteDesktopOnlyJar") {
    group = "build"
    description = "裁剪 sqlite-jdbc：保留 Java 类、services 与 Windows/macOS 原生库"
    val module = libs.sqlite.jdbc.get().module.toString()
    val version = libs.sqlite.jdbc.get().versionConstraint.toString()
    val source = configurations.detachedConfiguration(dependencies.create("$module:$version"))
    val outJar = layout.buildDirectory.file("sqlite-desktop/sqlite-jdbc-desktop.jar")
    inputs.files(source)
    outputs.file(outJar)
    doLast {
        val src = source.files.first { it.name.startsWith("sqlite-jdbc") && it.name.endsWith(".jar") }
        val out = outJar.get().asFile
        out.parentFile.mkdirs()
        ZipFile(src).use { zf ->
            ZipOutputStream(out.outputStream().buffered()).use { zo ->
                zf.entries().asSequence()
                    .filter { e ->
                        val n = e.name
                        n == "META-INF/MANIFEST.MF" ||
                            n.startsWith("META-INF/services/") ||
                            n.startsWith("META-INF/LICENSE") ||
                            n.startsWith("META-INF/NOTICE") ||
                            n.startsWith("META-INF/versions/") ||
                            n.startsWith("org/sqlite/native/Windows/") ||
                            n.startsWith("org/sqlite/native/Mac/") ||
                            (n.startsWith("org/sqlite/") && !n.startsWith("org/sqlite/native/"))
                    }
                    .forEach { e ->
                        zo.putNextEntry(ZipEntry(e.name))
                        if (!e.isDirectory) zf.getInputStream(e).copyTo(zo)
                        zo.closeEntry()
                    }
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            // material-icons-extended（36MB，仅用到 core 外 5 个图标）已移除，
            // 5 个图标本地复刻见 ui/LocalIcons.kt；core 图标随 material3 传递引入
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // sqlite-jdbc 运行时经 sqliteDesktopOnlyJar 裁剪：仅保留 Java 类与
                // Windows / macOS 原生库（Linux、FreeBSD 等占 18.35MB，本项目不发布这些平台）
                implementation(files(sqliteDesktopOnlyJar))
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

android {
    namespace = "org.xadfz.xianzhi.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 工作区路径含中文：Gradle 内置 Test worker 在本机通过 argfile 加载类路径时无法定位测试类，
// 改用 JavaExec 直接启动 JUnitCore 运行 desktop 测试（行为与标准 Test 任务一致）。
val desktopTestRunner = tasks.register<JavaExec>("desktopTestRunner") {
    group = "verification"
    description = "在 JVM(desktop) 上运行单元测试"
    val testCompilation = kotlin.targets.getByName("desktop").compilations.getByName("test")
    dependsOn(testCompilation.compileTaskProvider)
    // 测试要在开发机上真实读写 SQLite：主 classpath 里的 sqlite-jdbc 已被裁剪成只含
    // Windows/macOS 原生库，Linux 开发机需额外挂一份完整 jar 才能加载本机原生库
    val sqliteFullJar = configurations.detachedConfiguration(
        dependencies.create(libs.sqlite.jdbc.get().let { "${it.module}:${it.versionConstraint}" }),
    )
    classpath = files(testCompilation.output.allOutputs) +
        (testCompilation.runtimeDependencyFiles ?: files()) + sqliteFullJar
    mainClass.set("org.junit.runner.JUnitCore")
    doFirst {
        val classesRoot = testCompilation.output.classesDirs.first { it.exists() }
        val testClasses = classesRoot.walkTopDown()
            .filter { it.isFile && it.name.endsWith("Test.class") && "\$" !in it.name }
            .map {
                it.relativeTo(classesRoot).path
                    .replace('\\', '.').replace('/', '.')
                    .removeSuffix(".class")
            }
            .toList()
        require(testClasses.isNotEmpty()) { "未找到测试类" }
        args(testClasses)
    }
}
tasks.named("check") { dependsOn(desktopTestRunner) }
tasks.matching { it.name == "desktopTest" }.configureEach { enabled = false }
