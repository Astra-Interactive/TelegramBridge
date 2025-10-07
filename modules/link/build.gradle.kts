plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.mikro.extensions)
    implementation(libs.klibs.kstorage)
    implementation(libs.cache4k)
    compileOnly(libs.minecraft.luckperms)
    implementation(libs.jda)
    implementation(libs.bundles.exposed)
    implementation(libs.telegrambots.client)
    // Local
    implementation(projects.modules.core.api)
}
