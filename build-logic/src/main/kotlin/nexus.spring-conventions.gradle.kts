plugins {
    id("nexus.java-conventions")
    id("io.spring.dependency-management")
}

/*
 * Dependency management only — this plugin deliberately does NOT apply
 * org.springframework.boot. A module with the Boot plugin builds a fat jar and
 * stops being consumable as a dependency, so only the module that produces the
 * runnable application may apply it.
 */
dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
