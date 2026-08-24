plugins {
    id("nexus.spring-conventions")
}

description = "Discovery and lifecycle of external adapters. Empty until Phase 2."

dependencies {
    implementation(project(":nexus-application"))
}
