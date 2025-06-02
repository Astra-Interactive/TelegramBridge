plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.metro)
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
}
