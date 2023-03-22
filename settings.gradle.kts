import java.net.URI

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            library("libxposed-api", "io.github.libxposed", "api").version {
                branch = "master"
            }
            library("libxposed-interface", "io.github.libxposed", "interface").version {
                branch = "master"
            }
        }
    }
}

sourceControl {
    gitRepository(URI.create("https://github.com/libxposed/api.git")) {
        producesModule("io.github.libxposed:api")
    }
    gitRepository(URI.create("https://github.com/libxposed/service.git")) {
        producesModule("io.github.libxposed:interface")
    }
}

rootProject.name = "LSPosed"
include(
    ":app",
    ":core",
    ":daemon",
    ":dex2oat",
    ":hiddenapi:stubs",
    ":hiddenapi:bridge",
    ":magisk-loader",
    ":services:manager-service",
    ":services:daemon-service",
)
