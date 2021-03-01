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
val jarDestDir = "${projectDir}/template_override/system/framework/"
val isWindows = OperatingSystem.current().isWindows
val moduleId = "riru_lsposed"
val authors = "LSPosed Developers"

val riruModuleId = "lsposed"
val moduleMinRiruApiVersion = 10
val moduleMinRiruVersionName = "v23.0"
val moduleMaxRiruApiVersion = 10

val apiCode: Int by extra

val androidTargetSdkVersion: Int by extra
val androidMinSdkVersion: Int by extra
val androidBuildToolsVersion: String by extra
val androidCompileSdkVersion: Int by extra
val androidCompileNdkVersion: String by extra
val androidSourceCompatibility: JavaVersion by extra
val androidTargetCompatibility: JavaVersion by extra

val zipPathMagiskReleasePath: String by extra

val versionCode: Int by extra
val versionName: String by extra

dependencies {
    implementation("rikka.ndk:riru:10")
    implementation("com.android.tools.build:apksig:4.1.2")
    implementation(project(":sandhook-hooklib"))
    compileOnly(project(":hiddenapi-stubs"))
    compileOnly("androidx.annotation:annotation:1.1.0")
    implementation(project(":interface"))
    implementation(project(":hiddenapi-bridge"))
    implementation(project(":manager-service"))
}

android {
    compileSdkVersion(androidCompileSdkVersion)
    defaultConfig {
        applicationId("io.github.lsposed.lspd")
        minSdkVersion(androidMinSdkVersion)
        targetSdkVersion(androidTargetSdkVersion)
        multiDexEnabled = false

        buildFeatures {
            prefab = true
        }

        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                cppFlags("-std=c++17 -ffixed-x18 -Qunused-arguments -frtti -fomit-frame-pointer -fpie -fPIC")
                cFlags("-std=gnu99 -ffixed-x18 -Qunused-arguments -frtti -fomit-frame-pointer -fpie -fPIC")
                arguments("-DRIRU_MODULE_API_VERSION=$moduleMaxRiruApiVersion",
                        "-DRIRU_MODULE_VERSION=${extra["versionCode"]}",
                        "-DRIRU_MODULE_VERSION_NAME:STRING=\"${extra["versionName"]}\"")
            }
        }

        buildConfigField("int", "API_CODE", "$apiCode")
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
        buildConfigField("Integer", "VERSION_CODE", versionCode.toString())
    }

    lintOptions {
        isAbortOnError = false
        isCheckReleaseBuilds = false
    }

    buildTypes {
        named("debug") {
            externalNativeBuild {
                cmake {
                    cppFlags("-O0")
                    cFlags("-O0")
                }
            }
        }
        named("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            externalNativeBuild {
                cmake {
                    cppFlags("-fvisibility=hidden -fvisibility-inlines-hidden -Os -Wno-unused-value -fomit-frame-pointer -ffunction-sections -fdata-sections -Wl,--gc-sections -Wl,--strip-all -fno-unwind-tables")
                    cFlags("-fvisibility=hidden -fvisibility-inlines-hidden -Os -Wno-unused-value -fomit-frame-pointer -ffunction-sections -fdata-sections -Wl,--gc-sections -Wl,--strip-all -fno-unwind-tables")
                }
            }
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion(androidBuildToolsVersion)

    compileOptions {
        targetCompatibility(androidTargetCompatibility)
        sourceCompatibility(androidSourceCompatibility)
    }
}

afterEvaluate {

    android.applicationVariants.forEach { variant ->
        val variantCapped = variant.name.capitalize()
        val variantLowered = variant.name.toLowerCase()
        val zipFileName = "$moduleName-$versionName-$versionCode-$variantLowered.zip"

        delete(file(zipPathMagiskReleasePath))

        val prepareMagiskFilesTask = task("prepareMagiskFiles$variantCapped") {
            dependsOn("assemble$variantCapped")
            doFirst {
                copy {
                    from("$projectDir/tpl/module.prop.tpl")
                    into("$projectDir/template_override")
                    rename("module.prop.tpl", "module.prop")
                    expand("moduleId" to moduleId,
                            "versionName" to versionName,
                            "versionCode" to versionCode,
                            "authorList" to authors,
                            "apiCode" to apiCode,
                            "minApi" to "$moduleMinRiruApiVersion")
                    filter(mapOf("eol" to FixCrLfFilter.CrLf.newInstance("lf")), FixCrLfFilter::class.java)
                }
                copy {
                    from("${rootProject.projectDir}/README.md")
                    into(file(zipPathMagiskReleasePath))
                }
            }
            val libPathRelease = "${buildDir}/intermediates/cmake/$variantLowered/obj"
            val excludeList = arrayOf("util_functions.sh")
            doLast {
                val dexOutPath = if (variant.name.contains("release"))
                    "$buildDir/intermediates/dex/$variantLowered/minify${variantCapped}WithR8" else
                    "$buildDir/intermediates/dex/$variantLowered/mergeDex$variantCapped"
                copy {
                    from(dexOutPath) {
                        rename("classes.dex", "lspd.dex")
                    }
                    into(file(zipPathMagiskReleasePath + "system/framework/"))
                }
                copy {
                    from("${projectDir}/template_override")
                    into(zipPathMagiskReleasePath)
                    exclude(*excludeList)
                }
                copy {
                    from("${projectDir}/template_override")
                    into(zipPathMagiskReleasePath)
                    include("util_functions.sh")
                    filter { line ->
                        line.replace("%%%RIRU_MODULE_ID%%%", riruModuleId)
                                .replace("%%%RIRU_MIN_API_VERSION%%%", moduleMinRiruApiVersion.toString())
                                .replace("%%%RIRU_MIN_VERSION_NAME%%%", moduleMinRiruVersionName)
                    }
                    filter(mapOf("eol" to FixCrLfFilter.CrLf.newInstance("lf")), FixCrLfFilter::class.java)
                }
                copy {
                    include("riru_lspd")
                    rename("riru_lspd", "libriru_lspd.so")
                    from("$libPathRelease/armeabi-v7a")
                    into("$zipPathMagiskReleasePath/system/lib")
                }
                copy {
                    include("riru_lspd")
                    rename("riru_lspd", "libriru_lspd.so")
                    from("$libPathRelease/arm64-v8a")
                    into("$zipPathMagiskReleasePath/system/lib64")
                }
                copy {
                    include("riru_lspd")
                    rename("riru_lspd", "libriru_lspd.so")
                    from("$libPathRelease/x86")
                    into("$zipPathMagiskReleasePath/system_x86/lib")
                }
                copy {
                    include("riru_lspd")
                    rename("riru_lspd", "libriru_lspd.so")
                    from("$libPathRelease/x86_64")
                    into("$zipPathMagiskReleasePath/system_x86/lib64")
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
            val commands = arrayOf(android.adbExecutable, "push",
                    zipFileName,
                    "/data/local/tmp/")
            if (isWindows) {
                commandLine("cmd", "/c", commands.joinToString(" "))
            } else {
                commandLine(commands)
            }
        }
        task("flash${variantCapped}", Exec::class) {
            dependsOn(tasks.getByPath("push${variantCapped}"))
            workingDir("${projectDir}/release")
            val commands = arrayOf(android.adbExecutable, "shell", "su", "-c",
                    "magisk --install-module /data/local/tmp/${zipFileName}")
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
