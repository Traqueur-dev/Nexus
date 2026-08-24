plugins {
    id("nexus.java-conventions")
}

description = "Use cases and ports. Depends on the domain, and on no framework."

dependencies {
    // api, not implementation: the ports expose domain types in their signatures,
    // so anything depending on this module needs the domain on its compile path.
    api(project(":nexus-domain"))
}
