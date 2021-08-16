/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors
 */

import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.gradle.internal.dsl.BuildType
import java.nio.file.Paths
import java.time.Instant

plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs")
}

val androidTargetSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val defaultManagerPackageName: String by rootProject.extra
val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

val androidStoreFile: String? by rootProject
val androidStorePassword: String? by rootProject
val androidKeyAlias: String? by rootProject
val androidKeyPassword: String? by rootProject

android {
    compileSdk = androidCompileSdkVersion
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion = androidBuildToolsVersion

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = defaultManagerPackageName
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = verCode
        versionName = verName
        resourceConfigurations += arrayOf(
            "en", "zh-rCN", "zh-rTW", "zh-rHK", "ru", "uk", "nl", "ko", "fr", "de",
            "it", "pt", "es", "jp",
        )
        buildConfigField("long", "BUILD_TIME", Instant.now().epochSecond.toString())
    }

    compileOptions {
        targetCompatibility(androidTargetCompatibility)
        sourceCompatibility(androidSourceCompatibility)
    }

    lint {
        disable += "MissingTranslation"
        isAbortOnError = true
        isCheckReleaseBuilds = false
    }

    packagingOptions {
        resources {
            excludes += "META-INF/**"
            excludes += "okhttp3/**"
            excludes += "kotlin/**"
            excludes += "org/**"
            excludes += "**.properties"
            excludes += "**.bin"
        }
    }

    dependenciesInfo.includeInApk = false

    signingConfigs {
        create("config") {
            androidStoreFile?.also {
                storeFile = rootProject.file(it)
                storePassword = androidStorePassword
                keyAlias = androidKeyAlias
                keyPassword = androidKeyPassword
            }
        }
    }

    buildTypes {
        signingConfigs.named("config").get().also {
            debug {
                if (it.storeFile?.exists() == true) signingConfig = it
            }
            release {
                signingConfig = if (it.storeFile?.exists() == true) it
                else signingConfigs.named("debug").get()
                isMinifyEnabled = true
                (this as BuildType).isShrinkResources = true
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

androidComponents.onVariants { v ->
    val variant: ApplicationVariantImpl =
        if (v is ApplicationVariantImpl) v
        else (v as AnalyticsEnabledApplicationVariant).delegate as ApplicationVariantImpl
    variant.outputs.forEach {
        it.outputFileName.set("LSPosedManager-${verName}-${verCode}-${variant.name}.apk")
    }
}


val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
    val aapt2 = File(
        androidComponents.sdkComponents.sdkDirectory.get().asFile,
        "build-tools/${androidBuildToolsVersion}/aapt2"
    )
    val zip = Paths.get(
        project.buildDir.path,
        "intermediates",
        "optimized_processed_res",
        "release",
        "resources-release-optimize.ap_"
    )
    val optimized = File("${zip}.opt")
    val cmd = exec {
        commandLine(
            aapt2, "optimize",
            "--collapse-resource-names",
            "--enable-sparse-encoding",
            "-o", optimized,
            zip
        )
        isIgnoreExitValue = false
    }
    if (cmd.exitValue == 0) {
        delete(zip)
        optimized.renameTo(zip.toFile())
    }
}

tasks.whenTaskAdded {
    if (name == "optimizeReleaseResources") {
        finalizedBy(optimizeReleaseRes)
    }
}

dependencies {
    val glideVersion = "4.12.0"
    val okhttpVersion = "4.9.1"
    val navVersion = "2.3.5"
    annotationProcessor("com.github.bumptech.glide:compiler:$glideVersion")
    implementation("androidx.activity:activity:1.3.1")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("androidx.core:core:1.6.0")
    implementation("androidx.fragment:fragment:1.3.6")
    implementation("androidx.navigation:navigation-fragment:$navVersion")
    implementation("androidx.navigation:navigation-ui:$navVersion")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.slidingpanelayout:slidingpanelayout:1.2.0-alpha03")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.takisoft.preferencex:preferencex:1.1.0")
    implementation("com.takisoft.preferencex:preferencex-colorpicker:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("dev.rikka.rikkax.appcompat:appcompat:1.2.0-rc01")
    implementation("dev.rikka.rikkax.core:core:1.3.2")
    implementation("dev.rikka.rikkax.insets:insets:1.1.0")
    implementation("dev.rikka.rikkax.material:material:1.6.5")
    implementation("dev.rikka.rikkax.preference:simplemenu-preference:1.0.3")
    implementation("dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.2.2")
    implementation("dev.rikka.rikkax.widget:borderview:1.0.1")
    implementation("dev.rikka.rikkax.widget:switchbar:1.0.2")
    implementation("dev.rikka.rikkax.layoutinflater:layoutinflater:1.0.1")
    implementation("me.zhanghai.android.appiconloader:appiconloader-glide:1.3.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:2.0")
    implementation(project(":manager-service"))
}

configurations.all {
    resolutionStrategy {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "androidx.appcompat", module = "appcompat")
    }
}
