plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
}

dependencies {
    compileOnly(libs.minecraft.kyori.api)

    implementation(libs.jda)
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.core)

    implementation(projects.modules.core.api)
    implementation(projects.modules.messenger.discord)
    implementation(projects.modules.onboarding.api)

    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.minecraft.kyori.api)
    testImplementation(libs.minecraft.kyori.legacy)
    testImplementation(libs.minecraft.kyori.minimessage)
    testImplementation(libs.minecraft.kyori.plain)
    testImplementation(libs.tests.kotlin.test)

    testImplementation(testFixtures(projects.modules.core.api))
    testImplementation(testFixtures(projects.modules.messenger.discord))
    testImplementation(testFixtures(projects.modules.onboarding.api))
}
