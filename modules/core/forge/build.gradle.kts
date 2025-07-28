plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.forgegradle)
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    compileOnly(libs.minecraft.luckperms)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    // Local
    implementation(projects.modules.core.api)
}

dependencies {
    minecraft(
        "net.minecraftforge",
        "forge",
        "${libs.versions.minecraft.mojang.version.get()}-${libs.versions.minecraft.forge.version.get()}"
    )
}
minecraft {
    mappings("official", libs.versions.minecraft.mojang.version.get())
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
