pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Nexus"

include(
    "nexus-domain",
    "nexus-application",
    "nexus-infrastructure",
    "nexus-plugin-loader",
    "nexus-bootstrap",
)
