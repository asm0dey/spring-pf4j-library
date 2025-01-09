group = "com.github.asm0dey"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly(libs.pf4j)
}

tasks.test {
    useJUnitPlatform()
}