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
    compileOnly(libs.minecraft.luckperms)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.forge)

    implementation(projects.modules.core.api)
}

dependencies {
    minecraft {
        implementation(
            dependency(
                "net.minecraftforge:forge:" +
                    libs.versions.minecraft.minecraftforge.minecraft.get() +
                    "-" +
                    libs.versions.minecraft.minecraftforge.forge.get()
            )
        )
        mappings("official", libs.versions.minecraft.minecraftforge.minecraft.get())
    }
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
