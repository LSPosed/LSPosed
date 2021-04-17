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

val magiskDir: String by rootProject.extra

val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

dependencies {
    implementation("dev.rikka.ndk:riru:${moduleMinRiruVersionName}")
    implementation("com.android.tools.build:apksig:4.1.3")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.upb.cs.swt:axml:2.1.1")
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
                    "-fno-stack-protector",
                    "-fomit-frame-pointer",
                    "-Wno-builtin-macro-redefined",
                    "-Wl,--exclude-libs,ALL",
                    "-D__FILE__=__FILE_NAME__",
                    "-DRIRU_MODULE",
                    "-DRIRU_MODULE_API_VERSION=$moduleMaxRiruApiVersion",
                    """-DMODULE_NAME=\"$riruModuleId\""""
//                    "-DRIRU_MODULE_VERSION=$verCode", // this will stop ccache from hitting
//                    """-DRIRU_MODULE_VERSION_NAME=\"$verName\"""",
                )
                cppFlags("-std=c++20", *flags)
                cFlags("-std=c18", *flags)
                arguments(
                    "-DANDROID_STL=none",
                    "-DVERSION_CODE=$verCode",
                    "-DVERSION_NAME=$verName"
                )
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
                    arguments.addAll(arrayOf(
                        "-DCMAKE_CXX_FLAGS_DEBUG=-Og",
                        "-DCMAKE_C_FLAGS_DEBUG=-Og"
                    ))
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
                        "-Wno-unused-value",
                        "-ffunction-sections",
                        "-fdata-sections",
                        "-Wl,--gc-sections",
                        "-Wl,--strip-all",
                        "-fno-unwind-tables",
                        "-fno-asynchronous-unwind-tables"
                    )
                    cppFlags.addAll(flags)
                    cFlags.addAll(flags)
                    val configFlags = arrayOf(
                        "-Oz",
                        "-DNDEBUG"
                    ).joinToString(" ")
                    arguments.addAll(arrayOf(
                        "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=$configFlags",
                        "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_C_FLAGS_RELWITHDEBINFO=$configFlags"
                    ))
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
        Paths.get("${folder}${File.separator}${executable}${if (isWindows) ".exe" else ""}")
            .toFile()
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

        val prepareMagiskFilesTask = task("prepareMagiskFiles$variantCapped") {
            dependsOn("assemble$variantCapped")
            dependsOn(":app:assemble$variantCapped")
            doLast {
                delete {
                    delete(magiskDir)
                }
                copy {
                    from("${rootProject.projectDir}/README.md")
                    into(magiskDir)
                }
                copy {
                    from("$projectDir/magisk_module")
                    exclude("riru.sh", "module.prop")
                    into(magiskDir)
                }
                copy {
                    from("$projectDir/magisk_module")
                    include("module.prop")
                    into(magiskDir)
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
                    from("${projectDir}/magisk_module")
                    include("riru.sh")
                    into(magiskDir)
                    filter { line ->
                        line.replace("%%%RIRU_MODULE_LIB_NAME%%%", "lspd")
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
                            .replace(
                                "%%RIRU_MODULE_DEBUG%%",
                                if (variantLowered == "debug") "true" else "false"
                            )
                    }
                    filter(
                        mapOf("eol" to FixCrLfFilter.CrLf.newInstance("lf")),
                        FixCrLfFilter::class.java
                    )
                }
                copy {
                    from("${buildDir}/intermediates/cmake/$variantLowered/obj")
                    exclude("**/*.txt")
                    into("$magiskDir/lib")
                }
                val dexOutPath = if (variant.name.contains("release"))
                    "$buildDir/intermediates/dex/$variantLowered/minify${variantCapped}WithR8" else
                    "$buildDir/intermediates/dex/$variantLowered/mergeDex$variantCapped"
                copy {
                    from(dexOutPath)
                    rename("classes.dex", "lspd.dex")
                    into("$magiskDir/framework")
                }
                copy {
                    from("${project(":app").projectDir}/build/outputs/apk/${variantLowered}")
                    include("*.apk")
                    rename(".*\\.apk", "manager.apk")
                    into(magiskDir)
                }
                // generate sha1sum
                fileTree(magiskDir).matching {
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
            from(magiskDir)
        }

        val pushTask = task("push${variantCapped}", Exec::class) {
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
        val flashTask = task("flash${variantCapped}", Exec::class) {
            dependsOn(pushTask)
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
            dependsOn(flashTask)
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
