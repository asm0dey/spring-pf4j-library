import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spring.boot)
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
    implementation(project(":spring-meilisearch"))
    implementation(project(":seaweedfs-spring"))
    implementation(libs.seaweedfs)
    implementation(libs.commons.codec)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.mongo)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.meilisearch)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation(platform(libs.mongock.bom))
    implementation(libs.mongock.springboot)
    implementation(libs.mongock.mongodb.reactive.driver)
    implementation(libs.zip4j)
    implementation(platform(BOM_COORDINATES))
    kapt(libs.pf4j)
    implementation(libs.pf4j)
    runtimeOnly(libs.bulma)
    runtimeOnly(libs.font.awesome)
    runtimeOnly(libs.htmx.org)
    runtimeOnly(libs.htmx.ext.sse)
    runtimeOnly(libs.hyperscript)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.testcontainers.jupiter)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.mongo)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootJar>("bootJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<BootBuildImage>("bootBuildImage") {
    environment.putAll(
        mapOf(
            "BP_JVM_VERSION" to "24",
            "BP_NATIVE_IMAGE" to "false",
            "BP_JVM_CDS_ENABLED" to "true",
            "BP_SPRING_AOT_ENABLED" to "true",
            "BP_JVM_TYPE" to "JRE"
        )
    )
    imageName.set("asm0dey/${project.name}:${project.version}")
}
