plugins {
    kotlin("jvm")
    alias(libs.plugins.fabric.loom)
}

dependencies {
    mappings("net.fabricmc:yarn:${libs.versions.minecraft.fabric.yarn.get()}:v2")
    minecraft(libs.minecraft.fabric.mojang.get())
    modImplementation(libs.minecraft.fabric.loader.get())
    modImplementation(libs.minecraft.fabric.api.get())
}
