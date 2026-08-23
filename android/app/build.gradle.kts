plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The signing key is handed in by the build environment, never checked in.
// Without it the build still runs, but with a throwaway key — and an APK
// signed by a different key cannot be installed over an existing one.
val signingKeystore: String? = System.getenv("NCM_KEYSTORE")

android {
    signingConfigs {
        if (signingKeystore != null) {
            create("release") {
                storeFile = file(signingKeystore)
                storeType = "PKCS12"
                // PKCS12 carries one password for both the store and the key.
                storePassword = System.getenv("NCM_KEYSTORE_PASSWORD")
                keyPassword = System.getenv("NCM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NCM_KEY_ALIAS") ?: "ncm"
            }
        }
    }

    namespace = "io.github.khaledsabry255.ncmpermission"
    compileSdk = 35

    defaultConfig {
        // Deliberately different from the TWA wrapper (io.github.khaledsabry255.twa)
        // so this installs beside it instead of colliding with a different
        // signing key and refusing to install.
        applicationId = "io.github.khaledsabry255.ncmpermission"
        minSdk = 24
        targetSdk = 35
        versionCode = 17
        versionName = "3.0"
        resourceConfigurations += listOf("en", "ar")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // The stored key when the environment carries one, so every update
            // installs over the last. The debug key is only a fallback for a
            // build run without it.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
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
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    // Employee photos come straight from Supabase Storage over HTTPS.
    implementation("io.coil-kt:coil-compose:2.7.0")
}
