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
        classpath("com.android.tools.build:gradle:7.1.0-alpha11")
        classpath("org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.4.0-alpha08")
    }
}

val repo = FileRepository(rootProject.file(".git"))
val refId = repo.refDatabase.exactRef("refs/remotes/origin/master").objectId!!
val commitCount = Git(repo).log().add(refId).call().count()

val defaultManagerPackageName by extra("org.lsposed.manager")
val verCode by extra(commitCount + 4200)
val verName by extra("1.5.3")
val androidTargetSdkVersion by extra(31)
val androidMinSdkVersion by extra(27)
val androidBuildToolsVersion by extra("31.0.0")
val androidCompileSdkVersion by extra(31)
val androidCompileNdkVersion by extra("23.0.7599858")
val androidSourceCompatibility by extra(JavaVersion.VERSION_11)
val androidTargetCompatibility by extra(JavaVersion.VERSION_11)
val apiCode by extra(93)

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}
