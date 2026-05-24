plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
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
    compileOnly(libs.minecraft.forge.bus)
    compileOnly(libs.minecraft.forgeversion)
}
