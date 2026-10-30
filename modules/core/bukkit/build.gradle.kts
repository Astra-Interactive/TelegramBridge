plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.klibs.rules.gradle.linter)
    id("ru.astrainteractive.gradleplugin.java.version")
}

linter {
    unitKind.set("context")
    rootPackage.set("ru.astrainteractive.messagebridge.core.bukkit")
}

dependencies {
    compileOnly(libs.minecraft.paper.api)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.bukkit)

    implementation(projects.modules.core.api)
}
