group = "com.github.asm0dey"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly(libs.pf4j)
    compileOnly(libs.kotlinx.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
}