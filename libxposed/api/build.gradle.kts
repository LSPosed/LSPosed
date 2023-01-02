plugins {
    `java-library`
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.5.0")
    compileOnly(project(":stubs"))
}
