import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.file.DirectoryProperty

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// 本地数据工作区（仓库根 app/../数据），含 db 与图片；已从 git 移除，
// 由本机历史构建产物提供，缺失时需从 v0.1.0 安装包或 data v1 归档中取回
val dataDir = rootProject.projectDir.resolve("../数据")

// 发布签名：密钥库与口令均为本机文件（.gitignore 排除），口令从密钥文件读取
val keystoreFile = rootProject.projectDir.resolve("androidApp/release.keystore")
val keystorePwFile = rootProject.projectDir.resolve("androidApp/.keystore.pw")

/** 把对应版本的 db/图片同步到生成资产目录（Variant API 声明为生成源，依赖自动串联） */
abstract class EditionAssetsCopy : Copy() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}

// text  -> xianzhi.db + missing_chars
val copyTextEditionAssets = tasks.register<EditionAssetsCopy>("copyTextEditionAssets") {
    outputDir.set(layout.buildDirectory.dir("dataAssets/text"))
    destinationDir = outputDir.get().asFile
    from(dataDir.resolve("xianzhi.db"))
    from(dataDir.resolve("缺字图")) { into("missing_chars") }
}

// image -> xianzhi_img.db + missing_chars + content_images
val copyImageEditionAssets = tasks.register<EditionAssetsCopy>("copyImageEditionAssets") {
    outputDir.set(layout.buildDirectory.dir("dataAssets/image"))
    destinationDir = outputDir.get().asFile
    from(dataDir.resolve("xianzhi_img.db"))
    from(dataDir.resolve("缺字图")) { into("missing_chars") }
    from(dataDir.resolve("配图")) { into("content_images") }
}

android {
    namespace = "org.xadfz.xianzhi"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.xadfz.xianzhi"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        if (keystoreFile.exists() && keystorePwFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                val pw = keystorePwFile.readText().trim()
                storePassword = pw
                keyAlias = "xianzhi"
                keyPassword = pw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseCfg = signingConfigs.findByName("release")
            if (releaseCfg != null && releaseCfg.storeFile?.exists() == true) {
                signingConfig = releaseCfg
            }
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        create("text") {
            dimension = "edition"
            resValue("string", "app_name", "西安志")
        }
        create("image") {
            dimension = "edition"
            // 独立 applicationId，可与文字版并存；应用名与文字版统一
            applicationIdSuffix = ".image"
            resValue("string", "app_name", "西安志")
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

// 通过 Variant API 把数据目录注册为静态 assets 源（路径在 merge 前由对应版本
// 的 copy 任务经 pre<Variant>Build 保证就绪），所有消费任务均能正确看到
androidComponents {
    onVariants { variant ->
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val assetsDir = when {
            variant.name.startsWith("text") -> copyTextEditionAssets
            variant.name.startsWith("image") -> copyImageEditionAssets
            else -> null
        }
        if (assetsDir != null) {
            val preName = "pre${cap}Build"
            tasks.configureEach {
                if (name == preName) dependsOn(assetsDir)
            }
            variant.sources.assets?.addStaticSourceDirectory(
                assetsDir.get().outputDir.get().asFile
                    .relativeTo(project.projectDir).invariantSeparatorsPath,
            )
        }
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
