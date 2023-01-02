plugins {
    id("com.android.library")
}

android {
    compileSdk = 33
    buildToolsVersion = "33.0.1"
    namespace = "io.github.libxposed.service"

    defaultConfig {
        minSdk = 21
        targetSdk = 33
        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    buildFeatures {
        androidResources = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
