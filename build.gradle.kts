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
        maven("https://storage.googleapis.com/r8-releases/raw")
    }
    dependencies {
        classpath("com.android.tools:r8:3.0.30-dev")
        classpath("com.android.tools.build:gradle:7.0.0-alpha12")
        classpath("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
        classpath(kotlin("gradle-plugin", version = "1.4.32"))
    }
}

val repo = FileRepository(rootProject.file(".git"))
val refId = repo.refDatabase.exactRef("refs/remotes/origin/master").objectId!!
val commitCount = Git(repo).log().add(refId).call().count()

val defaultManagerPackageName by extra("org.lsposed.manager")
val verCode by extra(commitCount + 4200)
val verName by extra("v1.3.4")
val androidTargetSdkVersion by extra(30)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("30.0.3")
val androidCompileSdkVersion by extra(30)
val androidCompileNdkVersion by extra("22.0.7026061")
val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)
val apiCode by extra(93)
val zipPathMagiskReleasePath by extra(project(":core").projectDir.path + "/build/tmp/release/magisk/")

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
