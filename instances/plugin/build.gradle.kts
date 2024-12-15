import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.github.goooler.shadow")
    alias(libs.plugins.klibs.minecraft.shadow)
    alias(libs.plugins.klibs.minecraft.resource.processor)
}

dependencies {
    // Kotlin
    implementation(libs.bundles.kotlin)
    // Spigot dependencies
    compileOnly(libs.minecraft.paper.api)
    implementation(libs.minecraft.bstats)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.command.bukkit)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.menu.bukkit)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.klibs.kstorage)
    implementation(libs.minecraft.vaultapi)
    compileOnly(libs.driver.h2)
    compileOnly(libs.driver.jdbc)
    compileOnly(libs.driver.mysql)
    implementation(libs.kotlin.datetime)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    compileOnly(libs.minecraft.discordsrv)
    compileOnly(libs.minecraft.essentialsx)
    // Local
    implementation(projects.modules.bridge)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.messenger.bukkit)
    implementation(projects.modules.messenger.discord)
    implementation(projects.modules.messenger.telegram)
    implementation(projects.modules.core)
    implementation(projects.modules.coreBukkit)
    implementation(projects.modules.link)
}

minecraftProcessResource {
    bukkit(
        customProperties = mapOf(
            "libraries" to listOf(
                libs.driver.h2.get(),
                libs.driver.jdbc.get(),
                libs.driver.mysql.get(),
            ).joinToString("\",\"", "[\"", "\"]")
        )
    )
}

astraShadowJar {
    destination = File("_/run/media/makeevrserg/WDGOLD2TB/MinecraftServers/Servers/conf.smp/smp/plugins/")
        .takeIf { it.exists() }
        ?: File(rootDir, "jars")
    configureDefaults()
    requireShadowJarTask {
        relocate("org.bstats", requireProjectInfo.group)
        minimize {
            exclude(dependency(libs.exposed.jdbc.get()))
            exclude(dependency(libs.exposed.core.get()))
            exclude(dependency(libs.exposed.dao.get()))
        }
    }
}
