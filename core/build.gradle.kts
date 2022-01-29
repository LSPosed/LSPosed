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

import org.apache.commons.codec.binary.Hex
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*

plugins {
    id("com.android.application")
}

val moduleName = "LSPosed"
val moduleBaseId = "lsposed"
val authors = "LSPosed Developers"

val riruModuleId = "lsposed"
val moduleMinRiruApiVersion = 25
val moduleMinRiruVersionName = "25.0.1"
val moduleMaxRiruApiVersion = 25

val injectedPackageName: String by rootProject.extra
val injectedPackageUid: Int by rootProject.extra

val defaultManagerPackageName: String by rootProject.extra
val apiCode: Int by rootProject.extra
val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

val androidTargetSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

android {
    compileSdk = androidCompileSdkVersion
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion = androidBuildToolsVersion

    flavorDimensions += "api"

    buildFeatures {
        prefab = true
    }

    defaultConfig {
        applicationId = "org.lsposed.lspd"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = verCode
        versionName = verName
        multiDexEnabled = false

        externalNativeBuild {
            ndkBuild {
                arguments += "INJECTED_AID=$injectedPackageUid"
                arguments += "VERSION_CODE=$verCode"
                arguments += "VERSION_NAME=$verName"
                arguments += "-j${Runtime.getRuntime().availableProcessors()}"
            }
        }

        buildConfigField("int", "API_CODE", "$apiCode")
        buildConfigField(
            "String",
            "DEFAULT_MANAGER_PACKAGE_NAME",
            """"$defaultManagerPackageName""""
        )
        buildConfigField("String", "MANAGER_INJECTED_PKG_NAME", """"$injectedPackageName"""")
        buildConfigField("int", "MANAGER_INJECTED_UID", """$injectedPackageUid""")
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
    externalNativeBuild {
        ndkBuild {
            path("src/main/cpp/Android.mk")
        }
    }

    compileOptions {
        targetCompatibility(androidTargetCompatibility)
        sourceCompatibility(androidSourceCompatibility)
    }

    buildTypes {
        all {
            externalNativeBuild {
                ndkBuild {
                    arguments += "NDK_OUT=${File(buildDir, ".cxx/$name").absolutePath}"
                }
            }
        }
    }

    productFlavors {
        all {
            externalNativeBuild {
                ndkBuild {
                    arguments += "MODULE_NAME=${name.toLowerCase()}_$moduleBaseId"
                    arguments += "API=${name.toLowerCase()}"
                }
            }
            buildConfigField("String", "API", """"$name"""")
        }

        create("Riru") {
            dimension = "api"
            externalNativeBuild {
                ndkBuild {
                    arguments += "API_VERSION=$moduleMaxRiruApiVersion"
                }
            }
        }

        create("Zygisk") {
            dimension = "api"
            externalNativeBuild {
                ndkBuild {
                    arguments += "API_VERSION=1"
                }
            }
        }
    }

}


dependencies {
    // keep this dep since it affects ccache
    implementation("dev.rikka.ndk:riru:26.0.0")
    implementation("dev.rikka.ndk.thirdparty:cxx:1.2.0")
    implementation("io.github.vvb2060.ndk:dobby:1.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.upb.cs.swt:axml:2.1.2")
    compileOnly("androidx.annotation:annotation:1.3.0")
    compileOnly(project(":hiddenapi-stubs"))
    implementation(project(":hiddenapi-bridge"))
    implementation(project(":manager-service"))
    implementation(project(":daemon-service"))
}

val zipAll = task("zipAll") {

}

val apkDir: String
    get() = if (rootProject.extra.properties["android.injected.invoked.from.ide"] == "true") "intermediates" else "outputs"

fun afterEval() = android.applicationVariants.forEach { variant ->
    val variantCapped = variant.name.capitalize(Locale.ROOT)
    val variantLowered = variant.name.toLowerCase(Locale.ROOT)
    val buildTypeCapped = variant.buildType.name.capitalize(Locale.ROOT)
    val buildTypeLowered = variant.buildType.name.toLowerCase(Locale.ROOT)
    val flavorCapped = variant.flavorName!!.capitalize(Locale.ROOT)
    val flavorLowered = variant.flavorName!!.toLowerCase(Locale.ROOT)

    val magiskDir = "$buildDir/magisk/$variantLowered"

    val moduleId = "${flavorLowered}_$moduleBaseId"
    val zipFileName = "$moduleName-v$verName-$verCode-${flavorLowered}-$buildTypeLowered.zip"

    val prepareMagiskFilesTask = task<Sync>("prepareMagiskFiles$variantCapped") {
        dependsOn(
            "assemble$variantCapped",
            ":app:assemble$buildTypeCapped",
            ":daemon:assemble$buildTypeCapped"
        )
        into(magiskDir)
        from("${rootProject.projectDir}/README.md")
        from("$projectDir/magisk_module") {
            exclude("riru.sh", "module.prop", "customize.sh", "sepolicy.rule", "post-fs-data.sh")
        }
        from("$projectDir/magisk_module") {
            include("module.prop")
            expand(
                "moduleId" to moduleId,
                "versionName" to "v$verName",
                "versionCode" to verCode,
                "authorList" to authors,
                "updateJson" to "https://lsposed.github.io/LSPosed/release/${flavorLowered}.json",
                "requirement" to when (flavorLowered) {
                    "riru" -> "Requires Riru $moduleMinRiruVersionName or above installed"
                    "zygisk" -> "Requires Magisk 24.0+ and Zygisk enabled"
                    else -> "No further requirements"
                },
                "api" to flavorCapped
            )
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
        }
        from("$projectDir/magisk_module") {
            include("customize.sh", "post-fs-data.sh")
            val tokens = mapOf("FLAVOR" to flavorLowered)
            filter<ReplaceTokens>("tokens" to tokens)
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
        }
        if (flavorLowered == "riru") {
            from("${projectDir}/magisk_module") {
                include("riru.sh", "sepolicy.rule")
                val tokens = mapOf(
                    "RIRU_MODULE_LIB_NAME" to "lspd",
                    "RIRU_MODULE_API_VERSION" to moduleMaxRiruApiVersion.toString(),
                    "RIRU_MODULE_MIN_API_VERSION" to moduleMinRiruApiVersion.toString(),
                    "RIRU_MODULE_MIN_RIRU_VERSION_NAME" to moduleMinRiruVersionName,
                    "RIRU_MODULE_DEBUG" to if (buildTypeLowered == "debug") "true" else "false",
                )
                filter<ReplaceTokens>("tokens" to tokens)
                filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
            }
        }
        from("${project(":app").buildDir}/$apkDir/apk/${buildTypeLowered}") {
            include("*.apk")
            rename(".*\\.apk", "manager.apk")
        }
        from("${project(":daemon").buildDir}/$apkDir/apk/${buildTypeLowered}") {
            include("*.apk")
            rename(".*\\.apk", "daemon.apk")
        }
        into("lib") {
            from("${buildDir}/intermediates/stripped_native_libs/$variantCapped/out/lib")
            from("${project(":daemon").buildDir}/intermediates/ndkBuild/$buildTypeLowered/obj/local")
        }
        val dexOutPath = if (buildTypeLowered == "release")
            "$buildDir/intermediates/dex/$variantCapped/minify${variantCapped}WithR8" else
            "$buildDir/intermediates/dex/$variantCapped/mergeDex$variantCapped"
        into("framework") {
            from(dexOutPath)
            rename("classes.dex", "lspd.dex")
        }
        doLast {
            fileTree(magiskDir).visit {
                if (isDirectory) return@visit
                val md = MessageDigest.getInstance("SHA-256")
                file.forEachBlock(4096) { bytes, size ->
                    md.update(bytes, 0, size)
                }
                file(file.path + ".sha256").writeText(Hex.encodeHexString(md.digest()))
            }
        }
    }

    val zipTask = task<Zip>("zip${variantCapped}") {
        dependsOn(prepareMagiskFilesTask)
        archiveFileName.set(zipFileName)
        destinationDirectory.set(file("$projectDir/release"))
        from(magiskDir)
    }

    zipAll.dependsOn(zipTask)

    val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
    val pushTask = task<Exec>("push${variantCapped}") {
        dependsOn(zipTask)
        workingDir("${projectDir}/release")
        commandLine(adb, "push", zipFileName, "/data/local/tmp/")
    }
    val flashTask = task<Exec>("flash${variantCapped}") {
        dependsOn(pushTask)
        commandLine(
            adb, "shell", "su", "-c",
            "magisk --install-module /data/local/tmp/${zipFileName}"
        )
    }
    task<Exec>("flashAndReboot${variantCapped}") {
        dependsOn(flashTask)
        commandLine(adb, "shell", "reboot")
    }
}

afterEvaluate {
    afterEval()
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val killLspd = task<Exec>("killLspd") {
    commandLine(adb, "shell", "su", "-c", "killall", "lspd")
    isIgnoreExitValue = true
}
val pushDaemon = task<Exec>("pushDaemon") {
    dependsOn(":daemon:assembleDebug")
    workingDir("${project(":daemon").buildDir}/$apkDir/apk/debug")
    commandLine(adb, "push", "daemon-debug.apk", "/data/local/tmp/daemon.apk")
}
val pushDaemonNative = task<Exec>("pushDaemonNative") {
    dependsOn(":daemon:assembleDebug")
    doFirst {
        val abi: String = ByteArrayOutputStream().use { outputStream ->
            exec {
                commandLine(adb, "shell", "getprop", "ro.product.cpu.abi")
                standardOutput = outputStream
            }
            outputStream.toString().trim()
        }
        workingDir("${project(":daemon").buildDir}/intermediates/ndkBuild/debug/obj/local/$abi")
    }
    commandLine(adb, "push", "libdaemon.so", "/data/local/tmp/libdaemon.so")
}
val reRunDaemon = task<Exec>("reRunDaemon") {
    dependsOn(pushDaemon, pushDaemonNative, killLspd)
    // tricky to pass a minus number to avoid the injection warning
    commandLine(adb, "shell", "su", "-c", "sh `su -c magisk --path`/.magisk/modules/*_lsposed/service.sh --system-server-max-retry=-1&")
    isIgnoreExitValue = true
}
val tmpApk = "/data/local/tmp/lsp.apk"
val pushApk = task<Exec>("pushApk") {
    dependsOn(":app:assembleDebug")
    workingDir("${project(":app").buildDir}/$apkDir/apk/debug")
    commandLine(adb, "push", "app-debug.apk", tmpApk)
}
val openApp = task<Exec>("openApp") {
    commandLine(
        adb,
        "shell",
        "am",
        "start",
        "-a",
        "android.intent.action.MAIN",
        "-c",
        "org.lsposed.manager.LAUNCH_MANAGER",
        "com.android.shell/.BugreportWarningActivity"
    )
}
task<Exec>("reRunApp") {
    dependsOn(pushApk)
    commandLine(adb, "shell", "su", "-c", "mv -f $tmpApk /data/adb/lspd/manager.apk")
    isIgnoreExitValue = true
    finalizedBy(reRunDaemon)
}
