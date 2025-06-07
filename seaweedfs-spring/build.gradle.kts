plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    compileOnly(libs.seaweedfs)
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.seaweedfs)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
