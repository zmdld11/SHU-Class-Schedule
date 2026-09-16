plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.github.zmdld11.shuschedule"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.zmdld11.shuschedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "0.5.1"
        vectorDrawables { useSupportLibrary = true }
    }

    // 双包分发：full=默认版（内置明日方舟主题+.shutheme 主题包导入）；pure=纯净版（仅默认主题）
    flavorDimensions += "store"
    productFlavors {
        create("full") {
            dimension = "store"
        }
        create("pure") {
            dimension = "store"
            versionNameSuffix = "-pure"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 固定 release 签名（CI 经 KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS 注入，
            // 本地/未配置 secrets 时回退 debug 签名保证可构建）
            signingConfig = if (hasTextEnv("KEYSTORE_FILE") && hasTextEnv("KEYSTORE_PASSWORD") && hasTextEnv("KEY_ALIAS")) {
                signingConfigs.create("releaseFixed") {
                    storeFile = file(System.getenv("KEYSTORE_FILE"))
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEYSTORE_PASSWORD")
                }
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

private fun hasTextEnv(name: String): Boolean = !System.getenv(name).isNullOrBlank()

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
