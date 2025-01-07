plugins {
//    alias(libs.plugins.spring.dependencies)
}

group = "com.github.asm0dey"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api(libs.pf4j)
    api(libs.pf4j.spring){
        exclude(group="org.springframework", module="spring-core")
        exclude(group="org.springframework", module="spring-context")
        exclude(group="org.springframework", module="spring-beans")
        exclude(group="log4j")
        exclude(group="org.slf4j")
    }
    api("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-beans")
}

tasks.test {
    useJUnitPlatform()
}