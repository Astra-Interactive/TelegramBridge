plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly(libs.minecraft.luckperms)

    implementation(libs.cache4k)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.jda)
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.mikro.extensions)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.telegrambots.client)

    implementation(projects.modules.core.api)
}
