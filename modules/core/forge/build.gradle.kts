plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly(libs.minecraft.luckperms)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.forge)

    implementation(projects.modules.core.api)
}

dependencies {
    compileOnly(
        files(
            rootProject
                .file(".gradle")
                .resolve("mavenizer")
                .resolve("repo")
                .resolve("net")
                .resolve("minecraftforge")
                .resolve("forge")
                .resolve(libs.versions.minecraft.forgeversion.get())
                .resolve("forge-${libs.versions.minecraft.forgeversion.get()}.jar")
        )
    )
    compileOnly(libs.minecraft.brigadier)
    compileOnly(libs.minecraft.forgeversion)
    compileOnly(libs.minecraft.forge.bus)
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
