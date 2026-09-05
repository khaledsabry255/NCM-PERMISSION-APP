plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.khaledsabry255.permission"
    compileSdk = 35

    defaultConfig {
        // Both systems now live behind one launcher icon, so this is the only
        // NCM app there is. The id is kept from the permits wrapper so a phone
        // that already has that one upgrades to this instead of ending up with
        // two icons.
        applicationId = "io.github.khaledsabry255.permission"
        minSdk = 24
        targetSdk = 35
        versionCode = 17
        versionName = "4.0"
        resourceConfigurations += listOf("en", "ar")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI signs this with a debug key so the APK installs directly.
            // Replace with a real keystore before publishing to Play.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// The interface itself is the web app, loaded in a WebView, so the app needs
// nothing beyond an activity and the inset helpers.
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
