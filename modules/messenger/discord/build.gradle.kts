plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.metro)
}

dependencies {
    // Kotlin
    compileOnly(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)

    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)

    implementation(libs.kotlin.datetime)

    implementation(libs.discord.webhook)
    implementation(libs.jda)
    // Spigot
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core.api)
    implementation(projects.modules.link)
}
