import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import ru.astrainteractive.gradleplugin.property.model.Developer
import ru.astrainteractive.gradleplugin.property.util.requireJinfo
import ru.astrainteractive.gradleplugin.property.util.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.gradle.neoforgegradle)
    alias(libs.plugins.gradle.shadow)
}

repositories {
    mavenCentral()
    mavenLocal()
}
dependencies {
    // Kotlin
    shadow(libs.kotlin.coroutines.core)
    shadow(libs.kotlin.datetime)
    shadow(libs.kotlin.serialization.kaml)
    // AstraLibs
    shadow(libs.minecraft.astralibs.core)
    shadow(libs.minecraft.astralibs.command)
    shadow(libs.minecraft.astralibs.core.neoforge)

    shadow(libs.klibs.mikro.core)
    shadow(libs.klibs.mikro.extensions)
    shadow(libs.klibs.kstorage)

    shadow(libs.driver.h2)
    shadow(libs.exposed.jdbc)

    shadow(libs.minecraft.kyori.plain)
    shadow(libs.minecraft.kyori.legacy)
    shadow(libs.minecraft.kyori.gson)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    // Local
    shadow(projects.modules.messenger.api)
    shadow(projects.modules.messenger.discord)
    shadow(projects.modules.messenger.neoforge)
    shadow(projects.modules.messenger.telegram)
    shadow(projects.modules.core.api)
    shadow(projects.modules.core.neoforge)
    shadow(projects.modules.link)
}

tasks.named<ProcessResources>("processResources") {
    filteringCharset = "UTF-8"
    duplicatesStrategy = DuplicatesStrategy.WARN
    val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
    val resDirs = sourceSets
        .map(SourceSet::getResources)
        .map(SourceDirectorySet::getSrcDirs)
    from(resDirs) {
        include("META-INF/neoforge.mods.toml")
        expand(
            mapOf(
                "minecraft_version" to libs.versions.minecraft.mojang.version.get(),
                "minecraft_version_range" to listOf(libs.versions.minecraft.mojang.version.get())
                    .joinToString(","),
                "neo_version" to "neo_version",
                "neo_version_range" to "[${libs.versions.minecraft.neoforgeversion.get()},)",
                "mod_id" to requireProjectInfo.name.lowercase(),
                "mod_name" to requireProjectInfo.name,
                "mod_license" to "mod_license",
                "mod_version" to requireProjectInfo.versionString,
                "mod_authors" to requireProjectInfo.developersList
                    .map(Developer::id)
                    .joinToString(","),
                "mod_description" to requireProjectInfo.description
            )
        )
    }
}

val shadowJar by tasks.getting(ShadowJar::class) {
    mergeServiceFiles()
    dependsOn(tasks.named<ProcessResources>("processResources"))
    configurations = listOf(project.configurations.shadow.get())
    isReproducibleFileOrder = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = null as String?
    archiveVersion = requireProjectInfo.versionString
    archiveBaseName = "${requireProjectInfo.name}-${project.name}"
    destinationDirectory = rootDir
        .resolve("build")
        .resolve("neoforge")
        .resolve("mods")
        .takeIf(File::exists)
        ?: File(rootDir, "jars")
    dependencies {
        // Dependencies
        exclude(dependency("org.jetbrains:annotations"))
        // Root
        exclude("kotlin/**") // use kotlin-neoforge
        exclude("_COROUTINE/**")
        exclude("DebugProbesKt.bin")
        exclude("jetty-dir.css")
        exclude("license/**")
        exclude("**LICENCE**")
        exclude("**LICENSE**")
        // Other dependencies
        exclude("club/minnced/opus/**")
        exclude("co/touchlab/stately/**")
        exclude("com/google/**")
        exclude("com/ibm/icu/**")
        exclude("com/sun/**")
        exclude("google/protobuf/**")
        exclude("io/github/**")
        exclude("io/javalin/**")
        exclude("jakarta/servlet/**")
        exclude("javax/annotation/**")
        exclude("javax/servlet/**")
        exclude("natives/**")
        exclude("net/luckperms/**")
        exclude("nl/altindag/**")
        exclude("org/bouncycastle/**")
        exclude("org/checkerframework/**")
        exclude("org/conscrypt/**")
        exclude("org/eclipse/**")
        exclude("tomp2p/opuswrapper/**")
        // META
        exclude("META-INF/**.md")
        exclude("META-INF/**.MD")
        exclude("META-INF/**.txt**")
        exclude("META-INF/**LICENCE**")
        exclude("META-INF/com.android.tools/**")
        exclude("META-INF/gradle-plugins/**")
        exclude("META-INF/imports/**")
        exclude("META-INF/kotlin-reflection.kotlin_module")
        exclude("META-INF/license/**")
        exclude("META-INF/maven/**")
        exclude("META-INF/native-image/**")
        exclude("META-INF/native/**")
        exclude("META-INF/proguard/**")
        exclude("META-INF/rewrite/**")
        exclude("META-INF/services/kotlin.reflect.**")
        exclude("META-INF/versions/**")
    }

    // Be sure to relocate EXACT PACKAGES!!
    // For example, relocate org.some.package instead of org
    // Becuase relocation org will break other non-relocated dependencies such as org.minecraft
    // Don't relocate `org.jetbrains.exposed` and `kotlin`
    listOf(
        "ch.qos.logback",
        "club.minnced.discord",
        "club.minnced.opus",
        "com.arkivanov",
        "com.charleskorn.kaml",
        "com.fasterxml",
        "com.neovisionaries",
        "dev.icerock",
        "gnu.trove",
        "it.krzeminski",
        "javax.xml",
        "kotlinx",
        "net.dv8tion",
        "net.kyori",
        "net.thauvin",
        "okhttp3",
        "okio",
        "org.apache",
        "org.h2",
        "org.jetbrains.exposed",
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlinx",
        "org.json",
        "org.slf4j",
        "org.sqlite",
        "org.telegram",
        "org.w3c.css",
        "org.w3c.dom",
        "org.xml.sax",
        "ru.astrainteractive.astralibs",
        "ru.astrainteractive.klibs",
    ).forEach { pattern -> relocate(pattern, "${requireProjectInfo.group}.shade.$pattern") }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(requireJinfo.jtarget.majorVersion)

dependencies {
    compileOnly(libs.minecraft.neoforgeversion)
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
