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
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin

plugins {
    alias(libs.plugins.lsplugin.cmaker)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.nav.safeargs) apply false
}

cmaker {
    default {
        arguments.addAll(
            arrayOf(
                "-DEXTERNAL_ROOT=${File(rootDir.absolutePath, "external")}",
                "-DCORE_ROOT=${File(rootDir.absolutePath, "core/src/main/jni")}",
                "-DANDROID_STL=none"
            )
        )
        val flags = arrayOf(
            "-DINJECTED_AID=$injectedPackageUid",
            "-Wno-gnu-string-literal-operator-template",
            "-Wno-c++2b-extensions",
        )
        cFlags.addAll(flags)
        cppFlags.addAll(flags)
        abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    }
    buildTypes {
        if (it.name == "release") {
            arguments += "-DDEBUG_SYMBOLS_PATH=${
                layout.buildDirectory.dir("symbols").get().asFile.absolutePath
            }"
        }
    }
}

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/master") ?: 1) + 4200
val latestTag = repo?.latestTag?.removePrefix("v") ?: "1.0"

val injectedPackageName by extra("com.android.shell")
val injectedPackageUid by extra(2000)

val defaultManagerPackageName by extra("org.lsposed.manager")
val verCode by extra(commitCount)
val verName by extra(latestTag)
val androidTargetSdkVersion by extra(34)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("34.0.0")
val androidCompileSdkVersion by extra(34)
val androidCompileNdkVersion by extra("26.1.10909125")
val androidSourceCompatibility by extra(JavaVersion.VERSION_17)
val androidTargetCompatibility by extra(JavaVersion.VERSION_17)

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            ndkVersion = androidCompileNdkVersion
            buildToolsVersion = androidBuildToolsVersion

            externalNativeBuild {
                cmake {
                    version = "3.22.1+"
                }
            }

            defaultConfig {
                minSdk = androidMinSdkVersion
                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = verCode
                    versionName = verName
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }

            compileOptions {
                sourceCompatibility = androidSourceCompatibility
                targetCompatibility = androidTargetCompatibility
            }
        }
    }
    plugins.withType(JavaPlugin::class.java) {
        extensions.configure(JavaPluginExtension::class.java) {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
        }
    }
}
