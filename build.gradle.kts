plugins {
    // apply false: resolving the plugin here makes its version available to the
    // module that needs it, without applying it to the root project. Only the
    // module producing the runnable application may apply it — a module that
    // builds a boot jar cannot be consumed as a dependency by another.
    alias(libs.plugins.spring.boot) apply false
}

group = "fr.traqueur"
version = "1.0-SNAPSHOT"
