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
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.api.Git

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
        classpath(kotlin("gradle-plugin", version = "1.4.31"))
    }
}

val repo = FileRepository(rootProject.file(".git"))
val refId = repo.refDatabase.exactRef("refs/remotes/origin/master").objectId!!
val commitCount = Git(repo).log().add(refId).call().count()

allprojects {
    extra["versionCode"] = commitCount + 4200
    extra["versionName"] = "v1.2.0"
    extra["androidTargetSdkVersion"] = 30
    extra["androidMinSdkVersion"] = 27
    extra["androidBuildToolsVersion"] = "30.0.3"
    extra["androidCompileSdkVersion"] = 30
    extra["androidCompileNdkVersion"] = "22.0.7026061"
    extra["androidSourceCompatibility"] = JavaVersion.VERSION_1_8
    extra["androidTargetCompatibility"] = JavaVersion.VERSION_1_8
    extra["apiCode"] = 93
    extra["zipPathMagiskReleasePath"] = project(":core").projectDir.path + "/build/tmp/release/magisk/"

    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
        maven(url = "https://dl.bintray.com/rikkaw/Libraries")
    }
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}
