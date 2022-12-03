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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.nio.file.Paths
import org.gradle.internal.os.OperatingSystem

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    }
}
val (commitCount, latestTag) = FileRepositoryBuilder().setGitDir(rootProject.file(".git"))
    .runCatching {
        build().use { repo ->
            val git = Git(repo)
            val commitCount =
                git.log()
                    .add(repo.refDatabase.exactRef("refs/remotes/origin/master").objectId)
                    .call().count() + 4200
            val ver = git.describe()
                .setTags(true)
                .setAbbrev(0).call().removePrefix("v")
            commitCount to ver
        }
    }.getOrNull() ?: (1 to "1.0")

val injectedPackageName by extra("com.android.shell")
val injectedPackageUid by extra(2000)

val defaultManagerPackageName by extra("org.lsposed.manager")
val apiCode by extra(93)
val verCode by extra(commitCount)
val verName by extra(latestTag)
val androidTargetSdkVersion by extra(33)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("33.0.1")
val androidCompileSdkVersion by extra(33)
val androidCompileNdkVersion by extra("25.1.8937393")
val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}

fun findInPath(executable: String): String? {
    val pathEnv = System.getenv("PATH")
    return pathEnv.split(File.pathSeparator).map { folder ->
        Paths.get("${folder}${File.separator}${executable}${if (OperatingSystem.current().isWindows) ".exe" else ""}")
            .toFile()
    }.firstOrNull { path ->
        path.exists()
    }?.absolutePath
}

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        externalNativeBuild {
            cmake {
                version = "3.22.1+"
            }
        }

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName

            externalNativeBuild {
                cmake {
                    arguments += "-DEXTERNAL_ROOT=${File(rootDir.absolutePath, "external")}"
                    arguments += "-DCORE_ROOT=${File(rootDir.absolutePath, "core/src/main/jni")}"
                    abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                    val flags = arrayOf(
                        "-Wall",
                        "-Qunused-arguments",
                        "-Wno-gnu-string-literal-operator-template",
                        "-fno-rtti",
                        "-fvisibility=hidden",
                        "-fvisibility-inlines-hidden",
                        "-fno-exceptions",
                        "-fno-stack-protector",
                        "-fomit-frame-pointer",
                        "-Wno-builtin-macro-redefined",
                        "-Wno-unused-value",
                        "-D__FILE__=__FILE_NAME__",
                        "-DINJECTED_AID=$injectedPackageUid",
                    )
                    cppFlags("-std=c++20", *flags)
                    cFlags("-std=c18", *flags)
                    arguments(
                        "-DANDROID_STL=none",
                    )
                    findInPath("ccache")?.let {
                        println("Using ccache $it")
                        arguments += "-DANDROID_CCACHE=$it"
                    }
                }
            }
        }

        compileOptions {
            targetCompatibility(androidTargetCompatibility)
            sourceCompatibility(androidSourceCompatibility)
        }

        buildTypes {
            named("debug") {
                externalNativeBuild {
                    cmake {
                        arguments.addAll(
                            arrayOf(
                                "-DCMAKE_CXX_FLAGS_DEBUG=-Og",
                                "-DCMAKE_C_FLAGS_DEBUG=-Og",
                            )
                        )
                    }
                }
            }
            named("release") {
                externalNativeBuild {
                    cmake {
                        val flags = arrayOf(
                            "-Wl,--exclude-libs,ALL",
                            "-ffunction-sections",
                            "-fdata-sections",
                            "-Wl,--gc-sections",
                            "-fno-unwind-tables",
                            "-fno-asynchronous-unwind-tables",
                            "-flto=thin",
                            "-Wl,--thinlto-cache-policy,cache_size_bytes=300m",
                            "-Wl,--thinlto-cache-dir=${buildDir.absolutePath}/.lto-cache",
                        )
                        cppFlags.addAll(flags)
                        cFlags.addAll(flags)
                        val configFlags = arrayOf(
                            "-Oz",
                            "-DNDEBUG"
                        ).joinToString(" ")
                        arguments.addAll(
                            arrayOf(
                                "-DCMAKE_BUILD_TYPE=Release",
                                "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                                "-DDEBUG_SYMBOLS_PATH=${buildDir.absolutePath}/symbols",
                            )
                        )
                    }
                }
            }
        }
    }

    extensions.findByType(ApplicationExtension::class)?.lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
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
    }
}

fun Project.configureJavaExtension() {
    extensions.findByType(JavaPluginExtension::class.java)?.run {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
    plugins.withId("org.gradle.java-library") {
        configureJavaExtension()
    }
}
