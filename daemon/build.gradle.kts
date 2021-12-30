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

import com.android.build.gradle.BaseExtension
import com.android.ide.common.signing.KeystoreHelper
import java.io.PrintStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

plugins {
    id("com.android.application")
}

val daemonName = "LSPosed"

val injectedPackageName: String by rootProject.extra
val injectedPackageUid: Int by rootProject.extra

val agpVersion: String by rootProject.extra

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

    buildFeatures {
        prefab = true
    }

    defaultConfig {
        applicationId = "org.lsposed.daemon"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = verCode
        versionName = verName
        multiDexEnabled = false

        externalNativeBuild {
            ndkBuild {
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
        isAbortOnError = true
        isCheckReleaseBuilds = false
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
}

fun afterEval() = android.applicationVariants.forEach { variant ->
    val variantCapped = variant.name.capitalize(Locale.ROOT)
    val variantLowered = variant.name.toLowerCase(Locale.ROOT)

    tasks["merge${variantCapped}JniLibFolders"].enabled = false
    tasks["merge${variantCapped}NativeLibs"].enabled = false

    task<Jar>("generateApp${variantLowered}RFile") {
        dependsOn(":app:process${variantCapped}Resources")
        doLast {
            val rFile = JarFile(
                File(
                    project(":app").buildDir,
                    "intermediates/compile_and_runtime_not_namespaced_r_class_jar/${variantLowered}/R.jar"
                )
            )
            ZipOutputStream(
                FileOutputStream(
                    File(
                        project.buildDir,
                        "tmp/${variantLowered}R.jar"
                    )
                )
            ).use {
                for (entry in rFile.entries()) {
                    if (entry.name.startsWith("org/lsposed/manager")) {
                        it.putNextEntry(entry)
                        rFile.getInputStream(entry).transferTo(it)
                        it.closeEntry()
                    }
                }
            }
        }
    }

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
            PrintStream(outSrc).print(
                """
                |package org.lsposed.lspd.util;
                |public final class SignInfo {
                |    public static final byte[] CERTIFICATE = {${
                    certificateInfo.certificate.encoded.joinToString(",")
                }};
                |}""".trimMargin()
            )
        }
    }
    variant.registerJavaGeneratingTask(signInfoTask, outSrcDir)
}

afterEvaluate {
    afterEval()
}

dependencies {

    implementation("dev.rikka.ndk.thirdparty:cxx:1.2.0")
    implementation("com.android.tools.build:apksig:$agpVersion")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("androidx.annotation:annotation:1.3.0")
    compileOnly(project(":hiddenapi-stubs"))
    implementation(project(":hiddenapi-bridge"))
    implementation(project(":daemon-service"))
    implementation(project(":manager-service"))
    android.applicationVariants.all {
        "${name}Implementation"(files(File(project.buildDir, "tmp/${name}R.jar")) {
            builtBy("generateApp${name}RFile")
        })
    }
}
