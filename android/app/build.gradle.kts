plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.khaledsabry255.ncmpermission"
    compileSdk = 35

    defaultConfig {
        // Deliberately different from the TWA wrapper (io.github.khaledsabry255.twa)
        // so this installs beside it instead of colliding with a different
        // signing key and refusing to install.
        applicationId = "io.github.khaledsabry255.ncmpermission"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "2.0"
        resourceConfigurations += listOf("en", "ar")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed with the debug key so CI output installs directly from the
            // release page. A real keystore is only needed to publish on Play.
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
