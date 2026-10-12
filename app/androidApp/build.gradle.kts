plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// 仓库根「数据/」目录（app/../数据），含 db 与图片；CI/协作者从 Release 下载后放此
val dataDir = rootProject.projectDir.resolve("../数据")

// 构建前把文字版数据复制进 assets（存在才复制，db 不进 git）
val copyDbToAssets = tasks.register<Copy>("copyDbToAssets") {
    from(dataDir.resolve("xianzhi.db"))
    into(layout.projectDirectory.dir("src/main/assets"))
    onlyIf { dataDir.resolve("xianzhi.db").exists() }
}

val copyMissingCharsToAssets = tasks.register<Copy>("copyMissingCharsToAssets") {
    // assets 目录必须 ASCII：aapt2 在部分系统上对非 ASCII 路径有编码问题
    from(dataDir.resolve("缺字图"))
    into(layout.projectDirectory.dir("src/main/assets/missing_chars"))
    onlyIf { dataDir.resolve("缺字图").isDirectory }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(copyDbToAssets, copyMissingCharsToAssets)
}

android {
    namespace = "org.xadfz.xianzhi"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.xadfz.xianzhi"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
