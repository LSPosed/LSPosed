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

import com.android.build.api.dsl.ApplicationExtension
import com.android.ide.common.signing.KeystoreHelper
import java.io.PrintStream
import java.util.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.lsplugin.resopt)
}

val daemonName = "LSPosed"

val injectedPackageName: String by rootProject.extra
val injectedPackageUid: Int by rootProject.extra

val agpVersion: String by project

val defaultManagerPackageName: String by rootProject.extra

android {
    buildFeatures {
        prefab = true
    }

    defaultConfig {
        applicationId = "org.lsposed.daemon"

        buildConfigField(
            "String",
            "DEFAULT_MANAGER_PACKAGE_NAME",
            """"$defaultManagerPackageName""""
        )
        buildConfigField("String", "MANAGER_INJECTED_PKG_NAME", """"$injectedPackageName"""")
        buildConfigField("int", "MANAGER_INJECTED_UID", """$injectedPackageUid""")
    }

    buildTypes {
        all {
            externalNativeBuild {
                cmake {
                    arguments += "-DANDROID_ALLOW_UNDEFINED_SYMBOLS=true"
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }

    sourceSets {
        named("main") {
            res {
                srcDir(project(":app").file("src/common/res"))
            }
        }
    }
    namespace = "org.lsposed.daemon"
}

android.applicationVariants.all {
    val variantCapped = name.capitalize(Locale.ROOT)
    val variantLowered = name.toLowerCase(Locale.ROOT)

    val outSrcDir = file("$buildDir/generated/source/signInfo/${variantLowered}")
    val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
        dependsOn(":app:validateSigning${variantCapped}")
        val sign = rootProject.project(":app").extensions.getByType(ApplicationExtension::class.java).buildTypes.named(variantLowered).get().signingConfig
        val outSrc = file("$outSrcDir/org/lsposed/lspd/util/SignInfo.java")
        outputs.file(outSrc)
        doLast {
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
    registerJavaGeneratingTask(signInfoTask, outSrcDir)
}

dependencies {
    implementation(libs.libxposed.`interface`)
    implementation(libs.agp.apksig)
    implementation(libs.commons.lang3)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.services.managerService)
    compileOnly(libs.androidx.annotation)
    compileOnly(projects.hiddenapi.stubs)
}
