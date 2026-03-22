plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.datetime)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.neoforge)
    implementation(libs.minecraft.kyori.gson)
    implementation(libs.minecraft.kyori.legacy)
    implementation(libs.minecraft.kyori.plain)

    implementation(projects.modules.core.api)
    implementation(projects.modules.core.neoforge)
    implementation(projects.modules.link)
    implementation(projects.modules.messenger.api)
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
    compileOnly(libs.joml)
    compileOnly(libs.minecraft.datafixerupper)
    compileOnly(libs.minecraft.brigadier)
    compileOnly(libs.minecraft.neoforged.bus)
}
