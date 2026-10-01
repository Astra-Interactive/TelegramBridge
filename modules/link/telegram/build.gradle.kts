plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
}

dependencies {
    compileOnly(libs.minecraft.kyori.api)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.telegrambots.client)

    implementation(projects.modules.core.api)
    implementation(projects.modules.link.api)
    implementation(projects.modules.messenger.telegram.api)

    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.minecraft.kyori.api)
    testImplementation(libs.minecraft.kyori.legacy)
    testImplementation(libs.minecraft.kyori.minimessage)
    testImplementation(libs.minecraft.kyori.plain)
    testImplementation(libs.tests.kotlin.test)

    testImplementation(testFixtures(projects.modules.core.api))
    testImplementation(testFixtures(projects.modules.link.api))
    testImplementation(testFixtures(projects.modules.messenger.telegram.api))
}
