plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // Spigot dependencies
    compileOnly(libs.minecraft.paper.api)
    implementation(libs.minecraft.bstats)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.orm)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.menu.bukkit)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.klibs.kstorage)
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    compileOnly(libs.minecraft.discordsrv)
    compileOnly(libs.minecraft.essentialsx)
    implementation(projects.modules.messenger.api)
    // Test
    testImplementation(libs.bundles.testing.kotlin)
    testImplementation(kotlin("test"))
}
