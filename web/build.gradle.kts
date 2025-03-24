import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spring.boot)
//    alias(libs.plugins.spring.dependencies)
    alias(libs.plugins.graalvm)
    application
    kotlin("kapt")
}

group = "com.github.asm0dey"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

application {
    mainClass = "com.github.asm0dey.opdsko_spring.OpdskoSpringApplicationKt"
}
repositories {
    mavenCentral()
}

dependencies {
    api(project(":common"))
    developmentOnly(libs.spring.boot.docker.compose)
//    api(project(":epub-support", configuration = "shadow"))
//    api(project(":fb2-support", configuration = "shadow"))
//    api(project(":fb2-support"))
    implementation(project(":spring-meilisearch"))
    implementation(project(":seaweedfs-spring"))
    implementation(libs.seaweedfs)
    implementation(libs.commons.codec)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.reflections)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.mongo)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.meilisearch)
//    implementation(libs.tinylog)
//    implementation(libs.tinylog.kotlin)
//    implementation(libs.tinylog.slf4j)
    implementation(libs.zip4j)
    implementation(platform(BOM_COORDINATES))
    kapt(libs.pf4j)
    implementation(libs.pf4j)
    /*
        implementation(libs.pf4j.spring) {
            exclude("org.slf4j")
            exclude("org.springframework")
        }
    */
    runtimeOnly(libs.bulma)
    runtimeOnly(libs.font.awesome)
    runtimeOnly(libs.htmx.org)
    runtimeOnly(libs.hyperscript)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}




graalvmNative {
    toolchainDetection.set(false)
}

tasks.named<BootJar>("bootJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

/*
tasks.named<Task>("bootDistTar") {
    dependsOn(":fb2-support:shadowJar",":epub-support:shadowJar" )
    doLast {
        copy {
//        bootInf {
            from(project(":fb2-support").tasks.named("shadowJar"))
            from(project(":epub-support").tasks.named("shadowJar"))
            into("lib")
            println("HERE")
//        }
        }
    }
}
*/
