import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("nexus.spring-conventions")
    alias(libs.plugins.spring.boot)
}

description = "Assembles the application and produces the runnable jar."

dependencies {
    /*
     * The only module that sees every other one. Something has to know each
     * implementation in order to wire it to its port, and which types are
     * registered — assembly decisions that belong to no adapter.
     */
    implementation(project(":nexus-application"))
    implementation(project(":nexus-infrastructure"))
    implementation(project(":nexus-plugin-loader"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.amqp)

    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.webflux)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.rabbitmq)
    testImplementation(libs.awaitility)
}

tasks.named<BootRun>("bootRun") {
    args("--spring.profiles.active=dev")
}
