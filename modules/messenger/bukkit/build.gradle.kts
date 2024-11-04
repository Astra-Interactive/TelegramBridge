plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // Spigot dependencies
    compileOnly(libs.minecraft.paper.api)
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")
    implementation(libs.minecraft.bstats)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.menu.bukkit)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.klibs.kstorage)
    implementation(libs.minecraft.vaultapi)
    implementation(libs.kotlin.datetime)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    compileOnly(libs.minecraft.discordsrv)
    compileOnly(libs.minecraft.essentialsx)
    // Local
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core)
    implementation(projects.modules.coreBukkit)
}
