plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.kaml)
    implementation(libs.minecraft.astralibs.core)

    implementation(projects.modules.core.api)

    testFixturesImplementation(libs.kotlin.coroutines.core)
    testFixturesImplementation(libs.minecraft.astralibs.core)

    testImplementation(libs.minecraft.kyori.api)
    testImplementation(libs.tests.kotlin.test)
    testImplementation(testFixtures(projects.modules.core.api))
}
