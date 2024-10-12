import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.klibs.minecraft.shadow)
    application
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.kstorage)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation(libs.jda)
    // Local
    implementation(projects.modules.bridge)
    implementation(projects.modules.core)
    implementation(projects.modules.messenger.api)
}

setupShadow {
    configureDefaults()
    requireShadowJarTask {
        archiveBaseName.set("${requireProjectInfo.name}-bot")
        minimize {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.version.get()}"))
        }
    }
}

application {
    mainClass.set("${requireProjectInfo.group}.discord.${requireProjectInfo.name}")
}
