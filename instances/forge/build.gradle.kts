import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("io.github.goooler.shadow")
    alias(libs.plugins.klibs.minecraft.shadow)
    alias(libs.plugins.klibs.minecraft.resource.processor)
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.2.20")
    // Kotlin
    shadeImplementation(libs.bundles.kotlin)
    shadeImplementation(libs.bundles.exposed)
    // AstraLibs
    shadeImplementation(libs.minecraft.astralibs.core)
    shadeImplementation(libs.kotlin.serializationKaml)
    shadeImplementation(libs.klibs.mikro.core)
    shadeImplementation(libs.klibs.kstorage)
    shadeImplementation(libs.kotlin.datetime)
    shadeImplementation(libs.driver.h2)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    // Local
    shadeImplementation(projects.modules.bridge)
    shadeImplementation(projects.modules.messenger.api)
    shadeImplementation(projects.modules.messenger.discord)
    shadeImplementation(projects.modules.messenger.telegram)
    shadeImplementation(projects.modules.core)
    shadeImplementation(projects.modules.coreBukkit)
    shadeImplementation(projects.modules.link)
    shadeImplementation("net.kyori:adventure-text-serializer-plain:4.17.0")
    shadeImplementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    shadeImplementation("net.kyori:adventure-text-serializer-gson:4.17.0")
}

minecraft {
    mappings("official", "1.20.1")
}

configurations {
    apiElements {
        artifacts.clear()
    }
    runtimeElements {
        setExtendsFrom(emptySet())
        // Publish the jarJar
        artifacts.clear()
        outgoing.artifact(tasks.jarJar)
    }
}

val destination = File("_/media/makeevrserg/WDGOLD2TB/Minecraft Servers/server-docker-forge/data/mods")
    .takeIf(File::exists)
    ?: File(rootDir, "jars")

val reobfShadowJar = reobf.create("shadowJar")

astraShadowJar.configureManifest()

minecraftProcessResource.forge()

val shadowJar by tasks.getting(ShadowJar::class) {
    mergeServiceFiles()
    mustRunAfter(minecraftProcessResource.task)
    dependsOn(minecraftProcessResource.task)
    finalizedBy(reobfShadowJar)
    configurations = listOf(project.configurations.shadeImplementation.get())
    isReproducibleFileOrder = true
    archiveClassifier = null as String?
    archiveVersion = requireProjectInfo.versionString
    archiveBaseName = "${requireProjectInfo.name}-forge-shadow"
    destinationDirectory = destination

    dependencies {
        // deps
        exclude(dependency("org.jetbrains:annotations"))
        // deps paths
        exclude("co/touchlab/stately/**")
        exclude("club/minnced/opus/**")
        exclude("com/google/**")
        exclude("com/sun/**")
        exclude("google/protobuf/**")
        exclude("io/github/**")
        exclude("io/javalin/**")
        exclude("jakarta/servlet/**")
        exclude("javax/annotation/**")
        exclude("javax/servlet/**")
        exclude("natives/**")
        exclude("nl/altindag/**")
        exclude("org/eclipse/**")
        exclude("org/bouncycastle/**")
        exclude("org/checkerframework/**")
        exclude("org/conscrypt/**")
        exclude("tomp2p/opuswrapper/**")
        exclude("DebugProbesKt.bin")
        exclude("_COROUTINE/**")
        // meta
//        exclude("META-INF/services/**")
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/com.android.tools/**")
        exclude("META-INF/gradle-plugins/**")
        exclude("META-INF/maven/**")
        exclude("META-INF/proguard/**")
        exclude("META-INF/versions/**")
        exclude("META-INF/native/**")
        exclude("META-INF/**LICENCE**")
    }
    // Be sure to relocate EXACT PACKAGES!!
    // For example, relocate org.some.package instead of org
    // Becuase relocation org will break other non-relocated dependencies such as org.minecraft
    listOf(
        "com.fasterxml",
        "net.kyori",
        "org.h2",
        "com.neovisionaries",
        "gnu.trove",
        "org.json",
        "org.apache",
        "org.telegram",
        "okhttp3",
        "net.dv8tion",
        "okio",
        "org.slf4j",
        "kotlin",
        "kotlinx",
        "it.krzeminski",
        "net.thauvin",
        "org.jetbrains.exposed.dao",
        "org.jetbrains.exposed.exceptions",
        "org.jetbrains.exposed.sql",
        "org.jetbrains.exposed.jdbc",
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlinx",
        "com.charleskorn.kaml",
        "ru.astrainteractive.klibs",
        "ru.astrainteractive.astralibs",
        "club.minnced.discord",
        "club.minnced.opus"
    ).forEach { pattern -> relocate(pattern, "${requireProjectInfo.group}.shade.$pattern") }
}
