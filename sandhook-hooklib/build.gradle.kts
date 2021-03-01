plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    val androidTargetSdkVersion: Int by extra
    val androidCompileSdkVersion: Int by extra
    val androidMinSdkVersion: Int by extra
    val androidBuildToolsVersion: String by extra
    val androidSourceCompatibility: JavaVersion by extra
    val androidTargetCompatibility: JavaVersion by extra

    compileSdkVersion(androidCompileSdkVersion)

    defaultConfig {
        minSdkVersion(androidMinSdkVersion)
        targetSdkVersion(androidTargetSdkVersion)
        buildToolsVersion(androidBuildToolsVersion)
        versionCode(1)
        versionName("1.0")

        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility(androidSourceCompatibility)
        targetCompatibility(androidTargetCompatibility)
    }
}

dependencies {
    api(project(":sandhook-annotation"))
}
