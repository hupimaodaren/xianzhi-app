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
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.core)
}

compose.desktop {
    application {
        mainClass = "org.xadfz.xianzhi.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Zip)
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
