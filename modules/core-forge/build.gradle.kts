plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.forgegradle)
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    // Local
    implementation(projects.modules.core)
}

dependencies {
    minecraft(
        "net.minecraftforge",
        "forge",
        "${libs.versions.minecraft.version.get()}-${libs.versions.minecraft.forgeversion.get()}"
    )
}
minecraft {
    mappings("official", libs.versions.minecraft.version.get())
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
