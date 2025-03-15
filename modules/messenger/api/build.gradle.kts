plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {

    implementation(libs.minecraft.astralibs.core)
    // Kotlin
    implementation(libs.bundles.kotlin)
}
