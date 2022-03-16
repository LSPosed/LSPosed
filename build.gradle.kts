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
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("androidx.navigation.safeargs") apply false
    id("dev.rikka.tools.autoresconfig") apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r")
    }
}

val repo = FileRepository(rootProject.file(".git"))
val refId = repo.refDatabase.exactRef("refs/remotes/origin/master").objectId!!
val commitCount = Git(repo).log().add(refId).call().count()

val injectedPackageName by extra("com.android.shell")
val injectedPackageUid by extra(2000)

val defaultManagerPackageName by extra("org.lsposed.manager")
val apiCode by extra(93)
val verCode by extra(commitCount + 4200)
val verName by extra("1.7.2")
val androidTargetSdkVersion by extra(32)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("32.0.0")
val androidCompileSdkVersion by extra(32)
val androidCompileNdkVersion by extra("23.1.7779620")
val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}

subprojects {
    when (path) {
        ":app", ":core", ":daemon" -> apply(plugin = "com.android.application")
        ":daemon-servie", ":manager-service", ":interface", ":service" -> apply(plugin = "com.android.library")
    }
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName

            externalNativeBuild {
                cmake {
                    arguments += "-DEXTERNAL_ROOT=${File(rootDir.absolutePath, "external")}"
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
                        "-DVERSION_CODE=$verCode",
                        "-DVERSION_NAME=$verName",
                    )
                }
            }
        }

        (this as? com.android.build.api.dsl.ApplicationExtension)?.run {
            lint {
                abortOnError = true
                checkReleaseBuilds = false
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
                                "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=$configFlags",
                                "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_C_FLAGS_RELWITHDEBINFO=$configFlags",
                                "-DDEBUG_SYMBOLS_PATH=${buildDir.absolutePath}/symbols",
                            )
                        )
                    }
                }
            }
        }

    }
    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
        val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
            val aapt2 = File(
                androidComponents.sdkComponents.sdkDirectory.get().asFile,
                "build-tools/${androidBuildToolsVersion}/aapt2"
            )
            val zip = java.nio.file.Paths.get(
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
