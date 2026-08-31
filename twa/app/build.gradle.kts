plugins {
    id("com.android.application")
}

android {
    // The same identity as the wrapper already on the phones, so a new build
    // installs over it instead of arriving as a second, competing app.
    namespace = "io.github.khaledsabry255.twa"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.khaledsabry255.twa"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed after the build, with the key whose fingerprint is already
            // published in assetlinks.json — see CLAUDE.md.
            signingConfig = null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.androidbrowserhelper:androidbrowserhelper:2.5.0")
}
