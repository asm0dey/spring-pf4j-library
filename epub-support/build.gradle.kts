import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
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
    implementation(libs.epub4j)
    compileOnly(project(":common"))
    kapt(libs.pf4j)
    compileOnly(libs.pf4j)
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
            "Plugin-Id" to "epub-support",
            "Plugin-Version" to version,
            "Plugin-Provider" to "asm0dey",
        )
    }
}
