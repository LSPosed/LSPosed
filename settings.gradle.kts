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
            val agpVersion = version("agp", "7.4.2")
            val kotlinVersion = version("kotlin", "1.8.10")
            val navVersion = version("nav", "2.5.3")
            val appCenterVersion = version("appcenter", "5.0.0")
            val libxposedVersion = version("libxposed", "100")
            val glideVersion = version("glide", "4.15.0")
            val okhttpVersion = version("okhttp", "4.10.0")

            plugin("agp-lib", "com.android.library").versionRef(agpVersion)
            plugin("agp-app", "com.android.application").versionRef(agpVersion)
            plugin("kotlin", "org.jetbrains.kotlin.android").versionRef(kotlinVersion)
            plugin("nav-safeargs", "androidx.navigation.safeargs").versionRef(navVersion)
            plugin("autoresconfig", "dev.rikka.tools.autoresconfig").version("1.2.2")
            plugin("materialthemebuilder", "dev.rikka.tools.materialthemebuilder").version("1.3.3")
            plugin("lsplugin-resopt", "org.lsposed.lsplugin.resopt").version("1.3")
            plugin("lsplugin-apksign", "org.lsposed.lsplugin.apksign").version("1.1")
            plugin("lsplugin-cmaker", "org.lsposed.lsplugin.cmaker").version("1.0")
            plugin("lsplugin-jgit", "org.lsposed.lsplugin.jgit").version("1.0")

            library("appcenter-crashes", "com.microsoft.appcenter", "appcenter-crashes").versionRef(appCenterVersion)
            library("appcenter-analytics", "com.microsoft.appcenter", "appcenter-analytics").versionRef(appCenterVersion)

            library("libxposed-api", "io.github.libxposed", "api").versionRef(libxposedVersion)
            library("libxposed-service-interface", "io.github.libxposed", "service-interface").versionRef(libxposedVersion)


            library("rikkax-appcompat", "dev.rikka.rikkax.appcompat:appcompat:1.6.1")
            library("rikkax-core", "dev.rikka.rikkax.core:core:1.4.1")
            library("rikkax-insets", "dev.rikka.rikkax.insets:insets:1.3.0")
            library("rikkax-layoutinflater", "dev.rikka.rikkax.layoutinflater:layoutinflater:1.3.0")
            library("rikkax-material", "dev.rikka.rikkax.material:material:2.5.1")
            library("rikkax-material-preference", "dev.rikka.rikkax.material:material-preference:2.0.0")
            library("rikkax-parcelablelist", "dev.rikka.rikkax.parcelablelist:parcelablelist:2.0.1")
            library("rikkax-preference", "dev.rikka.rikkax.preference:simplemenu-preference:1.0.3")
            library("rikkax-recyclerview", "dev.rikka.rikkax.recyclerview:recyclerview-ktx:1.3.1")
            library("rikkax-widget-borderview", "dev.rikka.rikkax.widget:borderview:1.1.0")
            library("rikkax-widget-mainswitchbar", "dev.rikka.rikkax.widget:mainswitchbar:1.0.2")

            library("androidx-activity", "androidx.activity:activity:1.6.1")
            library("androidx-annotation", "androidx.annotation:annotation:1.6.0")
            library("androidx-browser", "androidx.browser:browser:1.5.0")
            library("androidx-constraintlayout", "androidx.constraintlayout:constraintlayout:2.1.4")
            library("androidx-core", "androidx.core:core:1.9.0")
            library("androidx-fragment", "androidx.fragment:fragment:1.5.5")
            library("androidx-navigation-fragment", "androidx.navigation", "navigation-fragment").versionRef(navVersion)
            library("androidx-navigation-ui", "androidx.navigation", "navigation-ui").versionRef(navVersion)
            library("androidx-preference", "androidx.preference:preference:1.2.0")
            library("androidx-recyclerview", "androidx.recyclerview:recyclerview:1.3.0")
            library("androidx-swiperefreshlayout", "androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")

            library("glide", "com.github.bumptech.glide", "glide").versionRef(glideVersion)
            library("glide-compiler", "com.github.bumptech.glide", "compiler").versionRef(glideVersion)

            library("okhttp", "com.squareup.okhttp3", "okhttp").versionRef(okhttpVersion)
            library("okhttp-dnsoverhttps", "com.squareup.okhttp3", "okhttp-dnsoverhttps").versionRef(okhttpVersion)
            library("okhttp-logging-interceptor", "com.squareup.okhttp3", "logging-interceptor").versionRef(okhttpVersion)


            library("agp-apksig", "com.android.tools.build", "apksig").versionRef(agpVersion)
            library("appiconloader", "me.zhanghai.android.appiconloader:appiconloader:1.5.0")
            library("axml", "de.upb.cs.swt:axml:2.1.3")
            library("commons-lang3", "org.apache.commons:commons-lang3:3.12.0")
            library("material", "com.google.android.material:material:1.8.0")
            library("gson", "com.google.code.gson:gson:2.10.1")
            library("hiddenapibypass", "org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef(kotlinVersion)
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        }
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
