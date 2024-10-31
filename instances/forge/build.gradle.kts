plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.orm)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.kotlin.datetime)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    // Local
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.messenger.discord)
    implementation(projects.modules.messenger.telegram)
    implementation(projects.modules.core)
    implementation(projects.modules.coreBukkit)
    implementation(projects.modules.link)
}

minecraft {
    mappings("official", "1.21.3")
}

dependencies {
    minecraft("net.minecraftforge:forge:1.21.3-53.0.0")
}
