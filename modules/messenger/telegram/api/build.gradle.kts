plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.astrainteractive.gradleplugin.detekt")
    id("ru.astrainteractive.gradleplugin.java.version")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.telegrambots.client)

    implementation(projects.modules.core.api)

    testImplementation(libs.tests.kotlin.test)

    testFixturesImplementation(libs.minecraft.astralibs.core)
    testFixturesImplementation(libs.telegrambots.client)
    testFixturesImplementation(projects.modules.core.api)
}
