plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.forgegradle)
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.coroutines.core)

    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.kotlin.datetime)
    implementation(libs.kyori.plain)
    implementation(libs.kyori.legacy)
    implementation(libs.kyori.gson)
    // Local
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core.api)
    implementation(projects.modules.core.forge)
    implementation(projects.modules.link)
}
dependencies {
    minecraft(
        "net.minecraftforge",
        "forge",
        "${libs.versions.minecraft.mojang.version.get()}-${libs.versions.minecraft.forgeversion.get()}"
    )
}
minecraft {
    mappings("official", libs.versions.minecraft.mojang.version.get())
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
