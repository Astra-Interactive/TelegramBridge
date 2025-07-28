plugins {
    kotlin("jvm")
    alias(libs.plugins.fabric.loom)
}

dependencies {
    mappings("net.fabricmc:yarn:${libs.versions.minecraft.fabric.yarn.get()}:v2")
    minecraft(libs.minecraft.fabric.mojang.get())
    modImplementation(libs.minecraft.fabric.loader.get())
    modImplementation(libs.minecraft.fabric.api.get())
    modImplementation("net.kyori:adventure-platform-fabric:5.9.0")
}

dependencies {
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.kotlin.coroutines.core)

    compileOnly(libs.minecraft.luckperms)
    compileOnly("net.kyori:adventure-platform-fabric:5.9.0")

    implementation(libs.minecraft.kyori.plain)
    implementation(libs.minecraft.kyori.legacy)
    implementation(libs.minecraft.kyori.gson)

    // Local
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.messenger.discord)
    implementation(projects.modules.messenger.telegram)
    implementation(projects.modules.core.api)
    implementation(projects.modules.link)
}
