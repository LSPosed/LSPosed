plugins {
    id("com.android.library")
    id("androidx.benchmark")
    kotlin("android")
}

val apiCode: Int by rootProject.extra
val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

val androidTargetSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra

android {
    namespace = "org.lsposed.lspd"
    compileSdk = androidCompileSdkVersion
    buildToolsVersion = androidBuildToolsVersion
    flavorDimensions += "test"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
    }

    buildTypes {
        release {
            isDefault = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "benchmark-proguard-rules.pro"
            )
        }
    }

    productFlavors {
        create("Benchmark") {
            isDefault = true
            dimension = "test"
            testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        }

        create("UnitTest") {
            dimension = "test"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}

dependencies {
    val testVersion = "1.4.0"
    val benchmarkVersion: String by rootProject.extra
    androidTestImplementation("io.kotest:kotest-assertions-core:5.1.0")
    androidTestImplementation("org.apache.commons:commons-lang3:3.12.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test:core-ktx:$testVersion")
    androidTestImplementation("androidx.test:runner:$testVersion")
    androidTestImplementation("androidx.benchmark:benchmark-junit4:$benchmarkVersion")
}