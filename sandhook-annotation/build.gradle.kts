plugins {
    `java-library`
}

java {
    sourceCompatibility = project.extra["androidSourceCompatibility"] as JavaVersion
    targetCompatibility = project.extra["androidTargetCompatibility"] as JavaVersion
}
