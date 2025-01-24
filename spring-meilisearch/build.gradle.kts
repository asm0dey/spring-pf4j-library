plugins {
    alias(libs.plugins.kotlin.spring)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.docker.compose)
    compileOnly(libs.meilisearch)
}

tasks.test {
    useJUnitPlatform()
}