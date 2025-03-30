import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.serialization)
    alias(libs.plugins.shadow)
    kotlin("kapt")
    java
}

group = "com.github.asm0dey"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":common"))
    kapt(libs.pf4j)
    compileOnly(libs.pf4j)
    implementation(libs.kotlinx.serialization.protobuf)
    compileOnly(libs.kotlinx.coroutines.core)
    implementation(libs.zip4j)

    // Test dependencies
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(project(":common"))
    testImplementation(libs.pf4j)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
        }
    }
    minimize()
    manifest {
        attributes(
            "Plugin-Id" to "inpx-support",
            "Plugin-Version" to version,
            "Plugin-Provider" to "asm0dey",
        )
    }
}
