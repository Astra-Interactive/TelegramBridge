plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.coroutines.core)

    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.neoforge)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation(libs.kotlin.datetime)
    implementation(libs.minecraft.kyori.plain)
    implementation(libs.minecraft.kyori.legacy)
    implementation(libs.minecraft.kyori.gson)
    // Local
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.core.api)
    implementation(projects.modules.core.neoforge)
    implementation(projects.modules.link)
}

dependencies {
    compileOnly(
        files(
            rootProject.project(projects.instances.neoforge.path)
                .file(".gradle")
                .resolve("repositories")
                .resolve("ng_dummy_ng")
                .resolve("net")
                .resolve("neoforged")
                .resolve("neoforge")
                .resolve(libs.versions.minecraft.neoforgeversion.get())
                .resolve("neoforge-${libs.versions.minecraft.neoforgeversion.get()}.jar")
        )
    )
    compileOnly(libs.minecraft.neoforgeversion)
    compileOnly("org.joml:joml:1.10.8")
    compileOnly("com.mojang:datafixerupper:8.0.16")
    compileOnly("com.mojang:brigadier:1.3.10")
    compileOnly("net.neoforged:bus:8.0.2")
}
