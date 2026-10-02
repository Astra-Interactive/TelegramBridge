plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.jda)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.core)

    testImplementation(libs.tests.kotlin.test)

    testFixturesImplementation(libs.jda)
}
