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

val verName: String by rootProject.extra
val verCode: Int by rootProject.extra

plugins {
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "org.lsposed.lspd.core"

    buildFeatures {
        androidResources = false
        buildConfig = true
    }

    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")

        buildConfigField("String", "FRAMEWORK_NAME", """"${rootProject.name}"""")
        buildConfigField("String", "VERSION_NAME", """"$verName"""")
        buildConfigField("long", "VERSION_CODE", """$verCode""")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

copy {
    from("src/main/jni/template/") {
        expand("VERSION_CODE" to "$verCode", "VERSION_NAME" to verName)
    }
    into("src/main/jni/src/")
}

dependencies {
    api(libs.libxposed.api)
    implementation(libs.commons.lang3)
    implementation(libs.axml)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.services.managerService)
    compileOnly(libs.androidx.annotation)
    compileOnly(projects.hiddenapi.stubs)
}
