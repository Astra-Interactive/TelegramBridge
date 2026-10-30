plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.klibs.rules.gradle.linter)
    id("ru.astrainteractive.gradleplugin.java.version")
}

linter {
    unitKind.set("context")
    rootPackage.set("ru.astrainteractive.messagebridge.command")
}

dependencies {
    compileOnly(libs.minecraft.brigadier)
    compileOnly(libs.minecraft.kyori.api)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.core)

    implementation(projects.modules.core.api)
}
