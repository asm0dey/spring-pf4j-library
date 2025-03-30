plugins {
    alias(libs.plugins.kotlin.jvm)
//    alias(libs.plugins.spring.dependencies)
    alias(libs.plugins.spring.boot) apply false
}
repositories {
    mavenCentral()
}
allprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}

val copyPlugins = tasks.register<Copy>("copyPlugins") {
    delete(fileTree("${layout.projectDirectory.asFile}/plugins") {
        include("*.jar")
    })
    dependsOn(
        ":fb2-support:shadowJar",
        ":epub-support:shadowJar",
        ":fb2-to-epub-converter:shadowJar",
        ":inpx-support:shadowJar"
    )
    from(project(":fb2-support").tasks.named("shadowJar"))
    from(project(":epub-support").tasks.named("shadowJar"))
    from(project(":inpx-support").tasks.named("shadowJar"))
    from(project(":fb2-to-epub-converter").tasks.named("shadowJar"))
    into("${layout.projectDirectory.asFile}/plugins")
    doLast {
        println("Plugins copied to ${layout.projectDirectory.asFile}/plugins")
    }
}

project(":web") {
    tasks.named("build") {
        dependsOn(copyPlugins)
    }
}
