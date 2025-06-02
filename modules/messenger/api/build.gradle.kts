plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.metro)
}

dependencies {

    implementation(libs.minecraft.astralibs.core)
    // Kotlin
    implementation(libs.bundles.kotlin)
}
