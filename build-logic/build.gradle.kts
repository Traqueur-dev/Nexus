plugins {
    `kotlin-dsl`
}

dependencies {
    // The convention plugins apply these, so their artifacts must be on
    // build-logic's compile classpath. The Boot plugin is here for its BOM
    // coordinates constant only — no module applies it except the one that
    // produces the runnable jar.
    implementation(libs.plugin.spring.boot)
    implementation(libs.plugin.spring.dependency.management)
}
