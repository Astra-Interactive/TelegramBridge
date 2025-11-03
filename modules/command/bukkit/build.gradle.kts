plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.coroutines.core)

    // Spigot dependencies
    compileOnly(libs.minecraft.paper.api)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.menu.bukkit)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.command.bukkit)

    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    // Local
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core.api)
    implementation(projects.modules.core.bukkit)
    implementation(projects.modules.link)
}
