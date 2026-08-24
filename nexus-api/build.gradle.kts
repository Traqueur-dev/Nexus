plugins {
    id("nexus.spring-conventions")
}

description = "Driving adapters: REST controllers and DTOs."

dependencies {
    implementation(project(":nexus-application"))

    implementation(libs.spring.boot.starter.web)

    /*
     * Jackson 2, needed only because EventDtoMapper serializes the context into a
     * String which Spring then serializes again — the double encoding tracked in
     * #32. Typing EventResponseDto.context as Context removes this dependency
     * entirely: the message converter already knows how to write it.
     */
    implementation(libs.jackson.databind)
}
