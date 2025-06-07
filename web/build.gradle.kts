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
version = "0.0.1"

application {
    mainClass = "com.github.asm0dey.opdsko_spring.OpdskoSpringApplicationKt"
}
repositories {
    mavenCentral()
}

dependencies {
    api(project(":common"))
    developmentOnly(libs.spring.boot.docker.compose)
    implementation(libs.commons.codec)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.xml.builder)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.meilisearch)
    implementation(libs.mongock.mongodb.reactive.driver)
    implementation(libs.mongock.springboot)
    implementation(libs.pf4j)
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.seaweedfs)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.mongo)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.zip4j)
    implementation(platform(BOM_COORDINATES))
    implementation(platform(libs.mongock.bom))
    implementation(project(":seaweedfs-spring"))
    implementation(project(":spring-meilisearch"))
    kapt(libs.pf4j)
    runtimeOnly(libs.bouncycastle)
    runtimeOnly(libs.bulma)
    runtimeOnly(libs.font.awesome)
    runtimeOnly(libs.htmx.ext.sse)
    runtimeOnly(libs.htmx.org)
    runtimeOnly(libs.hyperscript)
    runtimeOnly(libs.spectre.css)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.jupiter)
    testImplementation(libs.testcontainers.mongo)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.mockito.kotlin)
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

tasks.named<BootJar>("bootJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<BootBuildImage>("bootBuildImage") {
    environment.putAll(
        mapOf(
            "BP_JVM_VERSION" to "24",
            "BPL_JVM_CDS_ENABLED" to "true",
            "BP_JVM_JLINK_ENABLED" to "true",
            "BP_SPRING_AOT_ENABLED" to "true",
            "BP_JVM_TYPE" to "JRE"
        )
    )

    // Check for properties passed from the GitHub Actions workflow
    val customImageName = project.findProperty("bootBuildImage.imageName") as String?
    val customTags = project.findProperty("bootBuildImage.tags") as String?
    val shouldPublish = project.findProperty("bootBuildImage.publish") as String?

    // Set image name - use custom value if provided, otherwise use default
    if (customImageName != null) {
        imageName.set(customImageName)
    } else {
        imageName.set("ghcr.io/asm0dey/opdsko:${project.version}")
    }

    // Set tags - use custom value if provided, otherwise use default
    if (customTags != null) {
        tags.set(customTags.split(",").toList())
    } else {
        tags.add("ghcr.io/asm0dey/opdsko:latest")
    }

    // Enable publishing to a container registry if specified
    publish.set(shouldPublish?.toBoolean() ?: false)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}