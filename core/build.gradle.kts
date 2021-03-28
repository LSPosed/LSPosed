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

import org.apache.tools.ant.filters.FixCrLfFilter
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.daemon.common.toHexString
import java.nio.file.Paths

import java.security.MessageDigest

plugins {
    id("com.android.application")
    kotlin("android")
}

fun calcSha256(file: File): String {
    val md = MessageDigest.getInstance("SHA-256")
    file.forEachBlock(4096) { bytes, size ->
        md.update(bytes, 0, size)
    }
    return md.digest().toHexString()
}

val moduleName = "LSPosed"
val isWindows = OperatingSystem.current().isWindows
val moduleId = "riru_lsposed"
val authors = "LSPosed Developers"

val riruModuleId = "lsposed"
val moduleMinRiruApiVersion = 25
val moduleMinRiruVersionName = "25.0.0"
val moduleMaxRiruApiVersion = 25

val defaultManagerPackageName: String by rootProject.extra
val apiCode: Int by rootProject.extra

val androidTargetSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

val zipPathMagiskReleasePath: String by rootProject.extra

val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

dependencies {
    implementation("dev.rikka.ndk:riru:${moduleMinRiruVersionName}")
    implementation("com.android.tools.build:apksig:4.1.3")
    compileOnly(project(":hiddenapi-stubs"))
    compileOnly("androidx.annotation:annotation:1.2.0")
    implementation(project(":interface"))
    implementation(project(":hiddenapi-bridge"))
    implementation(project(":manager-service"))
}

android {
    compileSdkVersion(androidCompileSdkVersion)
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion(androidBuildToolsVersion)

    buildFeatures {
        prefab = true
    }

    defaultConfig {
        applicationId("org.lsposed.lspd")
        minSdkVersion(androidMinSdkVersion)
        targetSdkVersion(androidTargetSdkVersion)
        multiDexEnabled = false

        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                val flags = arrayOf(
                    "-ffixed-x18",
                    "-Qunused-arguments",
                    "-fno-rtti", "-fno-exceptions",
                    "-fomit-frame-pointer",
                    "-fpie", "-fPIC",
                    "-DRIRU_MODULE",
                    "-DRIRU_MODULE_API_VERSION=$moduleMaxRiruApiVersion",
                    "-DRIRU_MODULE_VERSION=$verCode",
                    """-DRIRU_MODULE_VERSION_NAME=\"$verName\"""",
                    """-DMODULE_NAME=\"$riruModuleId\""""
                )
                cppFlags("-std=c++20", *flags)
                cFlags("-std=c18", *flags)
                arguments("-DANDROID_STL=none")
                targets("lspd")
            }
        }

        buildConfigField("int", "API_CODE", "$apiCode")
        buildConfigField("String", "VERSION_NAME", "\"$verName\"")
        buildConfigField("Integer", "VERSION_CODE", verCode.toString())
        buildConfigField("String", "DEFAULT_MANAGER_PACKAGE_NAME", "\"$defaultManagerPackageName\"")
    }

    lint {
        isAbortOnError = false
        isCheckReleaseBuilds = false
    }

    buildTypes {
        named("debug") {
            externalNativeBuild {
                cmake {
                    val flags = arrayOf(
                        "-O0"
                    )
                    cppFlags.addAll(flags)
                    cFlags.addAll(flags)
                }
            }
        }
        named("release") {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")

            externalNativeBuild {
                cmake {
                    val flags = arrayOf(
                        "-fvisibility=hidden",
                        "-fvisibility-inlines-hidden",
                        "-Os",
                        "-Wno-unused-value",
                        "-ffunction-sections",
                        "-fdata-sections",
                        "-Wl,--gc-sections",
                        "-Wl,--strip-all",
                        "-fno-unwind-tables"
                    )
                    cppFlags.addAll(flags)
                    cFlags.addAll(flags)
                }
            }
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        targetCompatibility(androidTargetCompatibility)
        sourceCompatibility(androidSourceCompatibility)
    }
}

fun findInPath(executable: String): String? {
    val pathEnv = System.getenv("PATH")
    return pathEnv.split(File.pathSeparator).map { folder ->
        Paths.get("${folder}${File.separator}${executable}").toFile()
    }.firstOrNull { path ->
        path.exists()
    }?.absolutePath
}

task("buildLibcxx", Exec::class) {
    val ndkDir = android.ndkDirectory
    executable = "$ndkDir/${if (isWindows) "ndk-build.cmd" else "ndk-build"}"
    workingDir = projectDir
    findInPath("ccache")?.let {
        println("using ccache $it")
        environment("NDK_CCACHE", it)
        environment("USE_CCACHE", "1")
        environment("CCACHE_COMPILERCHECK", "content")
        environment("CCACHE_MAXSIZE", "100M")
    } ?: run {
        println("not using ccache")
    }

    setArgs(
        arrayListOf(
            "NDK_PROJECT_PATH=build/intermediates/ndk",
            "APP_BUILD_SCRIPT=$projectDir/src/main/cpp/external/libcxx/Android.mk",
            "APP_CPPFLAGS=-std=c++20",
            "APP_STL=none",
            "-j${Runtime.getRuntime().availableProcessors()}"
        )
    )
}

tasks.getByName("preBuild").dependsOn("buildLibcxx")

afterEvaluate {

    android.applicationVariants.forEach { variant ->
        val variantCapped = variant.name.capitalize()
        val variantLowered = variant.name.toLowerCase()
        val zipFileName = "$moduleName-$verName-$verCode-$variantLowered.zip"

        delete(file(zipPathMagiskReleasePath))

        val prepareMagiskFilesTask = task("prepareMagiskFiles$variantCapped") {
            dependsOn("assemble$variantCapped")
            dependsOn(":app:assemble$variantCapped")
            doFirst {
                copy {
                    from("$projectDir/tpl/module.prop.tpl")
                    into(zipPathMagiskReleasePath)
                    rename("module.prop.tpl", "module.prop")
                    expand(
                        "moduleId" to moduleId,
                        "versionName" to verName,
                        "versionCode" to verCode,
                        "authorList" to authors,
                        "minRiruVersionName" to moduleMinRiruVersionName
                    )
                    filter(
                        mapOf("eol" to FixCrLfFilter.CrLf.newInstance("lf")),
                        FixCrLfFilter::class.java
                    )
                }
                copy {
                    from("${rootProject.projectDir}/README.md")
                    into(file(zipPathMagiskReleasePath))
                }
            }
            val libPathRelease = "${buildDir}/intermediates/cmake/$variantLowered/obj"
            doLast {
                val dexOutPath = if (variant.name.contains("release"))
                    "$buildDir/intermediates/dex/$variantLowered/minify${variantCapped}WithR8" else
                    "$buildDir/intermediates/dex/$variantLowered/mergeDex$variantCapped"
                copy {
                    from(dexOutPath) {
                        rename("classes.dex", "lspd.dex")
                    }
                    into(file(zipPathMagiskReleasePath + "framework/"))
                }
                copy {
                    from("${projectDir}/template_override")
                    into(zipPathMagiskReleasePath)
                    exclude("riru.sh")
                }
                copy {
                    from("${projectDir}/template_override")
                    into(zipPathMagiskReleasePath)
                    include("riru.sh")
                    filter { line ->
                        line.replace("%%%RIRU_MODULE_ID%%%", riruModuleId)
                            .replace(
                                "%%%RIRU_MODULE_API_VERSION%%%",
                                moduleMaxRiruApiVersion.toString()
                            )
                            .replace(
                                "%%%RIRU_MODULE_MIN_API_VERSION%%%",
                                moduleMinRiruApiVersion.toString()
                            )
                            .replace(
                                "%%%RIRU_MODULE_MIN_RIRU_VERSION_NAME%%%",
                                moduleMinRiruVersionName
                            )
                    }
                    filter(
                        mapOf("eol" to FixCrLfFilter.CrLf.newInstance("lf")),
                        FixCrLfFilter::class.java
                    )
                }
                copy {
                    include("lspd")
                    rename("lspd", "liblspd.so")
                    from("$libPathRelease/armeabi-v7a")
                    into("$zipPathMagiskReleasePath/riru/lib")
                }
                copy {
                    include("lspd")
                    rename("lspd", "liblspd.so")
                    from("$libPathRelease/arm64-v8a")
                    into("$zipPathMagiskReleasePath/riru/lib64")
                }
                copy {
                    include("lspd")
                    rename("lspd", "liblspd.so")
                    from("$libPathRelease/x86")
                    into("$zipPathMagiskReleasePath/riru_x86/lib")
                }
                copy {
                    include("lspd")
                    rename("lspd", "liblspd.so")
                    from("$libPathRelease/x86_64")
                    into("$zipPathMagiskReleasePath/riru_x86/lib64")
                }
                copy {
                    from("${project(":app").projectDir}/build/outputs/apk/${variantLowered}")
                    include("*.apk")
                    rename(".*\\.apk", "manager.apk")
                    into(zipPathMagiskReleasePath)
                }
                // generate sha1sum
                fileTree(zipPathMagiskReleasePath).matching {
                    exclude("README.md", "META-INF")
                }.visit {
                    if (isDirectory) return@visit
                    file(file.path + ".sha256").writeText(calcSha256(file))
                }
            }
        }

        val zipTask = task("zip${variantCapped}", Zip::class) {
            dependsOn(prepareMagiskFilesTask)
            archiveFileName.set(zipFileName)
            destinationDirectory.set(file("$projectDir/release"))
            from(zipPathMagiskReleasePath)
        }

        task("push${variantCapped}", Exec::class) {
            dependsOn(zipTask)
            workingDir("${projectDir}/release")
            val commands = arrayOf(
                android.adbExecutable, "push",
                zipFileName,
                "/data/local/tmp/"
            )
            if (isWindows) {
                commandLine("cmd", "/c", commands.joinToString(" "))
            } else {
                commandLine(commands)
            }
        }
        task("flash${variantCapped}", Exec::class) {
            dependsOn(tasks.getByPath("push${variantCapped}"))
            workingDir("${projectDir}/release")
            val commands = arrayOf(
                android.adbExecutable, "shell", "su", "-c",
                "magisk --install-module /data/local/tmp/${zipFileName}"
            )
            if (isWindows) {
                commandLine("cmd", "/c", commands.joinToString(" "))
            } else {
                commandLine(commands)
            }
        }
        task("flashAndReboot${variantCapped}", Exec::class) {
            dependsOn(tasks.getByPath("flash${variantCapped}"))
            workingDir("${projectDir}/release")
            val commands = arrayOf(android.adbExecutable, "shell", "reboot")
            if (isWindows) {
                commandLine("cmd", "/c", commands.joinToString(" "))
            } else {
                commandLine(commands)
            }
        }
    }

}
