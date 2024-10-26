plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // Spigot dependencies
    compileOnly(libs.minecraft.paper.api)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.exposed)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.cache4k)

    compileOnly(libs.minecraft.luckperms)
    implementation(libs.jda)
    implementation(libs.bundles.exposed)
    implementation(libs.telegrambots.client)

    // Local
    implementation(projects.modules.core)
}
