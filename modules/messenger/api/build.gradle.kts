plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {

    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    // Kotlin
    implementation(libs.bundles.kotlin)
}
