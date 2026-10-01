plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
}

dependencies {
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.core)

    testImplementation(libs.minecraft.kyori.api)
    testImplementation(libs.tests.kotlin.test)
}
