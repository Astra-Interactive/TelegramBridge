plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    compileOnly(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.minecraft.vaultapi)
    implementation(libs.kotlin.datetime)
    implementation(libs.telegrambots.longpolling)
    implementation(libs.telegrambots.extensions)
    implementation(libs.telegrambots.client)
    // Spigot
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core)
    implementation(projects.modules.link)
}
