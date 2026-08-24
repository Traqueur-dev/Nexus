plugins {
    id("nexus.spring-conventions")
}

description = "Driven adapters: JPA, RabbitMQ, mail, serialization."

dependencies {
    implementation(project(":nexus-application"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.jackson.databind)

    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.starter.test)
}
