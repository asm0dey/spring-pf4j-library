plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.60.5"
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
