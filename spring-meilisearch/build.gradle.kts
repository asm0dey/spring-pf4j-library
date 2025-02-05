plugins {
    alias(libs.plugins.kotlin.spring)
    java
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.docker.compose)
    compileOnly(libs.meilisearch)
    testImplementation(libs.meilisearch)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.test {
    useJUnitPlatform()
}