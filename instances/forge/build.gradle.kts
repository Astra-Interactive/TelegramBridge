import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import ru.astrainteractive.gradleplugin.model.Developer
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("io.github.goooler.shadow")
    alias(libs.plugins.klibs.minecraft.shadow)
    alias(libs.plugins.klibs.minecraft.resource.processor)
}

val shade: Configuration by configurations.creating

val implementation: Configuration by configurations.getting {
    this.extendsFrom(shade)
}

repositories {
    mavenLocal {
        this.mavenContent {
            this.includeGroup("org.jetbrains.exposed")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:1.21.3-53.0.19")
    // Kotlin
    shade(libs.bundles.kotlin)
    shade(libs.bundles.exposed)
    // AstraLibs
    shade(libs.minecraft.astralibs.core)
    shade(libs.kotlin.serializationKaml)
    shade(libs.minecraft.astralibs.orm)
    shade(libs.klibs.mikro.core)
    shade(libs.klibs.kstorage)
    shade(libs.kotlin.datetime)
    shade(libs.driver.h2)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    // Local
    shade(projects.modules.bridge)
    shade(projects.modules.messenger.api)
    shade(projects.modules.messenger.discord)
    shade(projects.modules.messenger.telegram)
    shade(projects.modules.core)
    shade(projects.modules.coreBukkit)
    shade(projects.modules.link)
    shade("net.kyori:adventure-text-serializer-plain:4.17.0")
    shade("net.kyori:adventure-text-serializer-legacy:4.17.0")
    shade("net.kyori:adventure-text-serializer-gson:4.17.0")
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

val processResources = project.tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from(sourceSets.main.get().resources.srcDirs) {
        include("META-INF/mods.toml")
        include("mods.toml")
        expand(
            "modId" to requireProjectInfo.name.lowercase(),
            "version" to requireProjectInfo.versionString,
            "description" to requireProjectInfo.description,
            "displayName" to requireProjectInfo.name,
            "authors" to requireProjectInfo.developersList.map(Developer::id).joinToString(",")
        )
    }
}

val destination = File("_/media/makeevrserg/WDGOLD2TB/Minecraft Servers/server-docker-forge/data/mods")
    .takeIf(File::exists)
    ?: File(rootDir, "jars")

val reobfShadowJar = reobf.create("shadowJar")

val shadowJar by tasks.getting(ShadowJar::class) {
    mustRunAfter(processResources)
    dependsOn(processResources)
    configurations = listOf(shade)
//    shade.isTransitive = true

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
    mergeServiceFiles()
    manifest {
        attributes(
            "Specification-Title" to project.name,
            "Specification-Vendor" to requireProjectInfo.developersList.first().id,
            "Specification-Version" to project.version,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to requireProjectInfo.developersList.first().id
        )
    }
    isReproducibleFileOrder = true
    archiveClassifier.set(null as String?)
    archiveBaseName.set("${requireProjectInfo.name}-forge-shadow")
    destinationDirectory.set(destination)
    finalizedBy(reobfShadowJar)
}
