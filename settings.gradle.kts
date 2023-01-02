enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    val navVersion: String by settings
    val agpVersion: String by settings
    val kotlinVersion: String by settings
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version agpVersion
        id("com.android.application") version agpVersion
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("androidx.navigation.safeargs") version navVersion
        id("dev.rikka.tools.autoresconfig") version "1.2.2"
        id("dev.rikka.tools.materialthemebuilder") version "1.3.3"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LSPosed"
include(
    ":app",
    ":libxposed:api",
    ":libxposed:service",
    ":stubs",
    ":core",
    ":daemon",
    ":dex2oat",
    ":hiddenapi:stubs",
    ":hiddenapi:bridge",
    ":magisk-loader",
    ":services:manager-service",
    ":services:daemon-service",
)

project(":stubs").projectDir = file("libxposed/stubs")
