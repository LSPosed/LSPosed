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

import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.gradle.BaseExtension
import com.android.ide.common.signing.KeystoreHelper
import org.apache.commons.codec.binary.Hex
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens
import java.io.PrintStream
import java.security.MessageDigest

plugins {
    id("com.android.application")
}

val moduleName = "LSPosed"
val moduleId = "riru_lsposed"
val authors = "LSPosed Developers"

val riruModuleId = "lsposed"
val moduleMinRiruApiVersion = 25
val moduleMinRiruVersionName = "25.0.1"
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

val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

dependencies {
    implementation("dev.rikka.ndk:riru:26.0.0")
    implementation("dev.rikka.ndk.thirdparty:cxx:1.1.0")
    implementation("io.github.vvb2060.ndk:dobby:1.2")
    implementation("com.android.tools.build:apksig:7.0.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.upb.cs.swt:axml:2.1.1")
    compileOnly("androidx.annotation:annotation:1.2.0")
    compileOnly(project(":hiddenapi-stubs"))
    implementation(project(":hiddenapi-bridge"))
    implementation(project(":manager-service"))
}

android {
    compileSdk = androidCompileSdkVersion
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion = androidBuildToolsVersion

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
                arguments += "RIRU_MODULE_API_VERSION=$moduleMaxRiruApiVersion"
                arguments += "MODULE_NAME=$riruModuleId"
                arguments += "-j${Runtime.getRuntime().availableProcessors()}"
            }
        }

        buildConfigField("int", "API_CODE", "$apiCode")
        buildConfigField("String", "DEFAULT_MANAGER_PACKAGE_NAME", "\"$defaultManagerPackageName\"")
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

}

androidComponents.onVariants { v ->
    val variant: ApplicationVariantImpl =
        if (v is ApplicationVariantImpl) v
        else (v as AnalyticsEnabledApplicationVariant).delegate as ApplicationVariantImpl
    val variantCapped = variant.name.capitalize()
    val variantLowered = variant.name.toLowerCase()
    val zipFileName = "$moduleName-v$verName-$verCode-$variantLowered.zip"
    val magiskDir = "$buildDir/magisk/$variantLowered"

    afterEvaluate {
        val app = rootProject.project(":app").extensions.getByName<BaseExtension>("android")
        val outSrcDir = file("$buildDir/generated/source/signInfo/${variantLowered}")
        val outSrc = file("$outSrcDir/org/lsposed/lspd/util/SignInfo.java")
        val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
            dependsOn(":app:validateSigning${variantCapped}")
            outputs.file(outSrc)
            doLast {
                val sign = app.buildTypes.named(variantLowered).get().signingConfig
                outSrc.parentFile.mkdirs()
                val certificateInfo = KeystoreHelper.getCertificateInfo(
                    sign?.storeType,
                    sign?.storeFile,
                    sign?.storePassword,
                    sign?.keyPassword,
                    sign?.keyAlias
                )
                PrintStream(outSrc).apply {
                    println("package org.lsposed.lspd.util;")
                    println("public final class SignInfo {")
                    print("public static final byte[] CERTIFICATE = {")
                    val bytes = certificateInfo.certificate.encoded
                    print(bytes.joinToString(",") { it.toString() })
                    println("};")
                    println("}")
                }
            }
        }
        variant.variantData.registerJavaGeneratingTask(signInfoTask, arrayListOf(outSrcDir))
    }

    val prepareMagiskFilesTask = task("prepareMagiskFiles$variantCapped", Sync::class) {
        dependsOn("assemble$variantCapped")
        dependsOn(":app:assemble$variantCapped")
        into(magiskDir)
        from("${rootProject.projectDir}/README.md")
        from("$projectDir/magisk_module") {
            exclude("riru.sh", "module.prop")
        }
        from("$projectDir/magisk_module") {
            include("module.prop")
            expand(
                "moduleId" to moduleId,
                "versionName" to "v$verName",
                "versionCode" to verCode,
                "authorList" to authors,
                "minRiruVersionName" to moduleMinRiruVersionName
            )
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
        }
        from("${projectDir}/magisk_module") {
            include("riru.sh")
            val tokens = mapOf(
                "RIRU_MODULE_LIB_NAME" to "lspd",
                "RIRU_MODULE_API_VERSION" to moduleMaxRiruApiVersion.toString(),
                "RIRU_MODULE_MIN_API_VERSION" to moduleMinRiruApiVersion.toString(),
                "RIRU_MODULE_MIN_RIRU_VERSION_NAME" to moduleMinRiruVersionName,
                "RIRU_MODULE_DEBUG" to if (variantLowered == "debug") "true" else "false",
            )
            filter<ReplaceTokens>("tokens" to tokens)
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
        }
        from("${project(":app").buildDir}/outputs/apk/${variantLowered}") {
            include("*.apk")
            rename(".*\\.apk", "manager.apk")
        }
        into("lib") {
            from("${buildDir}/intermediates/stripped_native_libs/$variantLowered/out/lib")
        }
        val dexOutPath = if (variantLowered == "release")
            "$buildDir/intermediates/dex/$variantLowered/minify${variantCapped}WithR8" else
            "$buildDir/intermediates/dex/$variantLowered/mergeDex$variantCapped"
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

    val zipTask = task("zip${variantCapped}", Zip::class) {
        dependsOn(prepareMagiskFilesTask)
        archiveFileName.set(zipFileName)
        destinationDirectory.set(file("$projectDir/release"))
        from(magiskDir)
    }

    val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
    val pushTask = task("push${variantCapped}", Exec::class) {
        dependsOn(zipTask)
        workingDir("${projectDir}/release")
        commandLine(adb, "push", zipFileName, "/data/local/tmp/")
    }
    val flashTask = task("flash${variantCapped}", Exec::class) {
        dependsOn(pushTask)
        commandLine(
            adb, "shell", "su", "-c",
            "magisk --install-module /data/local/tmp/${zipFileName}"
        )
    }
    task("flashAndReboot${variantCapped}", Exec::class) {
        dependsOn(flashTask)
        commandLine(adb, "shell", "reboot")
    }
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val killLspd = task("killLspd", Exec::class) {
    commandLine(adb, "shell", "su", "-c", "killall -w lspd")
    isIgnoreExitValue = true
}
val pushLspd = task("pushLspd", Exec::class) {
    dependsOn("mergeDexDebug")
    workingDir("$buildDir/intermediates/dex/debug/mergeDexDebug")
    commandLine(adb, "push", "classes.dex", "/data/local/tmp/lspd.dex")
}
val pushLspdNative = task("pushLspdNative", Exec::class) {
    dependsOn("mergeDebugNativeLibs")
    workingDir("$buildDir/intermediates/merged_native_libs/debug/out/lib/arm64-v8a")
    commandLine(adb, "push", "libdaemon.so", "/data/local/tmp/libdaemon.so")
}
task("reRunLspd", Exec::class) {
    dependsOn(pushLspd)
    dependsOn(pushLspdNative)
    dependsOn(killLspd)
    commandLine(adb, "shell", "su", "-c", "sh /data/adb/modules/riru_lsposed/service.sh&")
    isIgnoreExitValue = true
}

val generateVersion = task("generateVersion", Copy::class) {
    inputs.property("VERSION_CODE", verCode)
    inputs.property("VERSION_NAME", verName)
    from("${projectDir}/src/main/cpp/main/template")
    include("config.cpp")
    expand("VERSION_CODE" to verCode, "VERSION_NAME" to verName)
    into("${projectDir}/src/main/cpp/main/src")
}
tasks.getByName("preBuild").dependsOn(generateVersion)
