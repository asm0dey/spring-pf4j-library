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
    implementation(libs.zip4j)
    implementation(libs.jsoup)
//    implementation(libs.tinylog)
//    implementation(libs.tinylog.kotlin)
//    runtimeOnly(libs.tinylog.slf4j)
    implementation(libs.commons.codec)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
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
            "Plugin-Id" to "fb2-support",
            "Plugin-Version" to version,
            "Plugin-Provider" to "asm0dey",
        )
    }
}
