plugins {
    id("nexus.spring-conventions")
}

description = "Driven adapters: JPA, RabbitMQ, mail, serialization."

dependencies {
    implementation(project(":nexus-application"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.mail)
    // The starter, not jackson-databind alone: this module contributes a
    // JsonMapperBuilderCustomizer, which lives in Boot's Jackson auto-configuration.
    implementation(libs.spring.boot.starter.jackson)

    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.starter.test)
}