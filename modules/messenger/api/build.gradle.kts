plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
    id("java-test-fixtures")
}

dependencies {
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.minecraft.astralibs.core)

    testFixturesImplementation(libs.klibs.mikro.core)
    testFixturesImplementation(libs.kotlin.coroutines.core)

    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.tests.kotlin.test)
}
