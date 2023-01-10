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
    id("com.android.library")
}

android {
    namespace = "org.lsposed.lspd.core"

    buildFeatures {
        androidResources = false
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
    api("io.github.libxposed:api:100")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.upb.cs.swt:axml:2.1.3")
    compileOnly("androidx.annotation:annotation:1.5.0")
    compileOnly(projects.hiddenapi.stubs)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.services.managerService)
}
