plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
}
