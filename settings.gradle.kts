enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    val navVersion: String by settings
    val agpVersion: String by settings
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version agpVersion
        id("com.android.application") version agpVersion
        id("androidx.navigation.safeargs") version navVersion
        id("dev.rikka.tools.autoresconfig") version "1.1.0"
        id("dev.rikka.tools.materialthemebuilder") version "1.0.0"
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
    ":core",
    ":daemon",
    ":hiddenapi:stubs",
    ":hiddenapi:bridge",
    ":magisk-loader",
    ":services:manager-service",
    ":services:daemon-service",
    ":services:xposed-service:interface",
)

buildCache { local { removeUnusedEntriesAfterDays = 1 } }
