plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
}

dependencies {
    compileOnly(libs.minecraft.brigadier)
    compileOnly(libs.minecraft.kyori.api)
    compileOnly(libs.minecraft.luckperms)

    implementation(libs.cache4k)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.mikro.extensions)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.core)

    implementation(projects.modules.core.api)
    implementation(projects.modules.link.api)

    testImplementation(libs.driver.h2)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.minecraft.brigadier)
    testImplementation(libs.minecraft.kyori.api)
    testImplementation(libs.minecraft.kyori.legacy)
    testImplementation(libs.minecraft.kyori.minimessage)
    testImplementation(libs.minecraft.kyori.plain)
    testImplementation(libs.minecraft.luckperms)
    testImplementation(libs.tests.kotlin.test)

    testImplementation(testFixtures(projects.modules.core.api))
    testImplementation(testFixtures(projects.modules.link.api))
}
