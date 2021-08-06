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

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
    }
}

val commitCount = "git rev-list --count origin/master".exec().toInt()

val defaultManagerPackageName by extra("org.lsposed.manager")
val verCode by extra(commitCount + 4200)
val verName by extra("v1.4.8")
val androidTargetSdkVersion by extra(31)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("31.0.0")
val androidCompileSdkVersion by extra(31)
val androidCompileNdkVersion by extra("22.1.7171670")
val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)
val apiCode by extra(93)

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}
