plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.minecraftforge.gradle")
}

repositories {
    minecraft.mavenizer(this)
    mavenCentral()
    mavenLocal()
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {
    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.datetime)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.forge)
    implementation(libs.minecraft.kyori.gson)
    implementation(libs.minecraft.kyori.legacy)
    implementation(libs.minecraft.kyori.plain)

    implementation(projects.modules.core.api)
    implementation(projects.modules.core.forge)
    implementation(projects.modules.link)
    implementation(projects.modules.messenger.api)
}

dependencies {
    compileOnly(minecraft.dependency(libs.minecraft.forgeversion.get()))
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
