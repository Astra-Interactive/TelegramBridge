plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.datetime)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.telegrambots.client)
    implementation(libs.telegrambots.extensions)
    implementation(libs.telegrambots.longpolling)

    implementation(projects.modules.core.api)
    implementation(projects.modules.link)
    implementation(projects.modules.messenger.api)
}
