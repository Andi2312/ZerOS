plugins {
    id("com.android.application") version "8.5.2"
}

android {
    namespace = "org.zeroc.about"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.zeroc.about"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Material Components 1.x = Material Design 2 (MD3 starts at 1.5).
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
