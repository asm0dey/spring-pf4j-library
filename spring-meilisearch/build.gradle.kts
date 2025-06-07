plugins {
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
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}