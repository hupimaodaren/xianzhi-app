import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqlite.jdbc)
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
    classpath = files(testCompilation.output.allOutputs) + (testCompilation.runtimeDependencyFiles ?: files())
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
