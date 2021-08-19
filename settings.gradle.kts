dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jcenter.bintray.com")
    }
}

rootProject.name = "LSPosed"
include(
    ":core",
    ":hiddenapi-stubs",
    ":app",
    ":service",
    ":interface",
    ":hiddenapi-bridge",
    ":manager-service"
)

val serviceRoot = "service"
project(":interface").projectDir = file("$serviceRoot${File.separator}interface")
project(":service").projectDir = file("$serviceRoot${File.separator}service")

buildCache { local { removeUnusedEntriesAfterDays = 1 } }
