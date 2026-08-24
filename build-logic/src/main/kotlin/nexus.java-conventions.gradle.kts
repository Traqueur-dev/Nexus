import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    // java-library, not java: modules expose domain types in their public
    // signatures, so they need the api configuration to pass that on.
    `java-library`
}

group = "fr.traqueur"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // Every module tests the same way. Declared here with explicit versions so a
    // module needs no Spring plugin just to run a unit test — which is what lets
    // nexus-domain stay free of Spring even in its build.
    "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testImplementation"(libs.findLibrary("assertj-core").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    /*
     * Keep parameter names in the bytecode. Spring resolves @PathVariable,
     * @RequestParam and constructor bindings by parameter name when no name is
     * given explicitly, and fails at runtime without this.
     *
     * The Spring Boot plugin adds this flag itself, which is why it was implicit
     * while everything lived in one module that applied it. Modules that do not
     * apply the Boot plugin — every one of them except bootstrap — need it here.
     */
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
