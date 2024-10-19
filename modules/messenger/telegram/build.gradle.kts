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
    implementation(libs.klibs.kstorage)
    compileOnly(libs.minecraft.vaultapi)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.telegram:telegrambots-longpolling:7.10.0")
    implementation("org.telegram:telegrambots-extensions:7.10.0")
    implementation("org.telegram:telegrambots-client:7.10.0")
    // Spigot
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core)
}
