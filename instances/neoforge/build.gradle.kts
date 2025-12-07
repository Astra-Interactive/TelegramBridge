import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import ru.astrainteractive.gradleplugin.model.Developer
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireJinfo
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.neoforgegradle)
    alias(libs.plugins.gradle.shadow)
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.coroutines.core)
    // AstraLibs
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.core.neoforge)
    implementation(libs.kotlin.serialization.kaml)
    implementation(libs.klibs.mikro.core)
    implementation(libs.klibs.mikro.extensions)
    implementation(libs.klibs.kstorage)
    implementation(libs.kotlin.datetime)
    implementation(libs.driver.h2)
    // Spigot
    compileOnly(libs.minecraft.luckperms)
    // Local
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.messenger.discord)
    implementation(projects.modules.messenger.neoforge)
    implementation(projects.modules.messenger.telegram)
    implementation(projects.modules.core.api)
    implementation(projects.modules.core.neoforge)
    implementation(projects.modules.link)
    implementation(libs.kyori.plain)
    implementation(libs.kyori.legacy)
    implementation(libs.kyori.gson)
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

val destination = rootDir
    .resolve("build")
    .resolve("neoforge")
    .resolve("mods")
    .takeIf(File::exists)
    ?: File(rootDir, "jars")
val shadowJar by tasks.getting(ShadowJar::class) {
    mergeServiceFiles()
    dependsOn(tasks.named<ProcessResources>("processResources"))
    configurations = listOf(project.configurations.shadow.get())
    isReproducibleFileOrder = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = null as String?
    archiveVersion = requireProjectInfo.versionString
    archiveBaseName = "${requireProjectInfo.name}-${project.name}"
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

dependencies {
    compileOnly(libs.minecraft.neoforgeversion)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(requireJinfo.jtarget.majorVersion)

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}
