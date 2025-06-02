plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.forgegradle)
    alias(libs.plugins.metro)
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.kotlin.datetime)
    implementation(libs.minecraft.kyori.plain)
    implementation(libs.minecraft.kyori.legacy)
    implementation(libs.minecraft.kyori.gson)
    // Local
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core.api)
    implementation(projects.modules.core.forge)
    implementation(projects.modules.link)
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
