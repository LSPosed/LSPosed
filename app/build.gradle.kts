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

import java.time.Instant

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.nav.safeargs)
    alias(libs.plugins.autoresconfig)
    alias(libs.plugins.materialthemebuilder)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.lsplugin.apksign)
}

apksign {
    storeFileProperty = "androidStoreFile"
    storePasswordProperty = "androidStorePassword"
    keyAliasProperty = "androidKeyAlias"
    keyPasswordProperty = "androidKeyPassword"
}

val defaultManagerPackageName: String by rootProject.extra

android {
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = defaultManagerPackageName
        buildConfigField("long", "BUILD_TIME", Instant.now().epochSecond.toString())
    }

    packaging {
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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }

    sourceSets {
        named("main") {
            res {
                srcDirs("src/common/res")
            }
        }
    }
    namespace = defaultManagerPackageName
}

autoResConfig {
    generateClass = true
    generateRes = false
    generatedClassFullName = "org.lsposed.manager.util.LangList"
    generatedArrayFirstItem = "SYSTEM"
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F",
            "Sakura" to "FF9CA8"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = true
}

dependencies {
    annotationProcessor(libs.glide.compiler)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.rikkax.appcompat)
    implementation(libs.rikkax.core)
    implementation(libs.rikkax.insets)
    implementation(libs.rikkax.material)
    implementation(libs.rikkax.material.preference)
    implementation(libs.rikkax.preference)
    implementation(libs.rikkax.recyclerview)
    implementation(libs.rikkax.widget.borderview)
    implementation(libs.rikkax.widget.mainswitchbar)
    implementation(libs.rikkax.layoutinflater)
    implementation(libs.appiconloader)
    implementation(libs.hiddenapibypass)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(projects.services.managerService)

    debugImplementation(libs.appcenter.analytics)
    debugImplementation(libs.appcenter.crashes)
}

configurations.all {
    exclude("org.jetbrains", "annotations")
    exclude("androidx.appcompat", "appcompat")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}
