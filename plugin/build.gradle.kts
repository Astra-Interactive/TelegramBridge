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
    implementation(libs.minecraft.astralibs.orm)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.menu.bukkit)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.klibs.kstorage)
    compileOnly(libs.minecraft.vaultapi)
    compileOnly(libs.driver.h2)
    compileOnly(libs.driver.jdbc)
    compileOnly(libs.driver.mysql)
    implementation("org.telegram:telegrambots-longpolling:7.10.0")
    implementation("org.telegram:telegrambots-extensions:7.10.0")
    implementation("org.telegram:telegrambots-client:7.10.0")
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    compileOnly(libs.minecraft.discordsrv)
    compileOnly(libs.minecraft.essentialsx)
}

minecraftProcessResource {
    spigotResourceProcessor.process()
}

setupShadow {
    destination = File("/home/makeevrserg/Desktop/server/data/plugins")
        .takeIf { it.exists() }
        ?: File(rootDir, "jars")
    configureDefaults()
    requireShadowJarTask {
        minimize {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.version.get()}"))
        }
    }
}
