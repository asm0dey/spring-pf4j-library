plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.60.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "opdsko-spring"
include("epub-support")
include("web")
include("common")
include("fb2-support")
include("spring-meilisearch")
include("seaweedfs-spring")
include("fb2-to-epub-converter")
include("inpx-support")
