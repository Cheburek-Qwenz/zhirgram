import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Секреты: сначала local.properties (локально), потом переменные окружения (CI).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String =
    localProps.getProperty(name) ?: System.getenv(name) ?: ""

android {
    namespace = "com.qwenz.zhirgram"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.qwenz.zhirgram"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Только arm64: libtdjni.so собирается под него (см. .github/workflows/build-tdlib.yml)
        ndk { abiFilters += listOf("arm64-v8a") }

        buildConfigField("int", "TG_APP_ID", (secret("TG_APP_ID").ifEmpty { "0" }))
        buildConfigField("String", "TG_APP_HASH", "\"${secret("TG_APP_HASH")}\"")
    }

    signingConfigs {
        create("release") {
            val store = secret("STORE_FILE")
            if (store.isNotEmpty()) {
                storeFile = file(store)
                storePassword = secret("STORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // нет ключа: подпишется debug-ключом, чтобы сборка не падала
            signingConfig = if (secret("STORE_FILE").isNotEmpty())
                signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        // нативная libtdjni.so должна лежать в APK несжатой/как есть
        jniLibs { useLegacyPackaging = false }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
