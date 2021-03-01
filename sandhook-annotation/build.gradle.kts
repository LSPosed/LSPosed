plugins {
    `java-library`
}

java {
    sourceCompatibility = extra["androidSourceCompatibility"] as JavaVersion
    targetCompatibility = extra["androidTargetCompatibility"] as JavaVersion
}
