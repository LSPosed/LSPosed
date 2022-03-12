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
    ":core",
    ":hiddenapi:stubs",
    ":hiddenapi:bridge",
    ":app",
    ":service",
    ":interface",
    ":manager-service",
    ":daemon",
    ":daemon-service"
)

val serviceRoot = "service"
project(":interface").projectDir = file("$serviceRoot${File.separator}interface")
project(":service").projectDir = file("$serviceRoot${File.separator}service")

buildCache { local { removeUnusedEntriesAfterDays = 1 } }
