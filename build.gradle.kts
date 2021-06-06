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
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0-beta03")
        classpath("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.4.0-alpha02")
        classpath(kotlin("gradle-plugin", version = "1.4.32"))
    }
}

val repo = FileRepository(rootProject.file(".git"))
val refId = repo.refDatabase.exactRef("refs/remotes/origin/master").objectId!!
val commitCount = Git(repo).log().add(refId).call().count()

val defaultManagerPackageName by extra("org.lsposed.manager")
val verCode by extra(commitCount + 4200)
val verName by extra("v1.4.5")
val androidTargetSdkVersion by extra(30)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("30.0.3")
val androidCompileSdkVersion by extra("android-S")
val androidCompileNdkVersion by extra("22.1.7171670")
val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)
val apiCode by extra(93)

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jcenter.bintray.com")
    }
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}
