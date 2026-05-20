import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import ru.astrainteractive.gradleplugin.property.model.Developer
import ru.astrainteractive.gradleplugin.property.util.requireJinfo
import ru.astrainteractive.gradleplugin.property.util.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.gradle.forgegradle)
    alias(libs.plugins.gradle.forgerenamer)
    alias(libs.plugins.gradle.shadow)
}

repositories {
    minecraft.mavenizer(this)
    mavenCentral()
    mavenLocal()
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {
    // Kotlin
    shadow(libs.kotlin.coroutines.core)
    shadow(libs.kotlin.datetime)
    shadow(libs.kotlin.serialization.kaml)
    // AstraLibs
    shadow(libs.minecraft.astralibs.core)
    shadow(libs.minecraft.astralibs.command)
    shadow(libs.minecraft.astralibs.core.forge)

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
    shadow(projects.modules.messenger.forge)
    shadow(projects.modules.messenger.telegram)
    shadow(projects.modules.core.api)
    shadow(projects.modules.core.forge)
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
        include("META-INF/mods.toml")
        expand(
            mapOf(
                "minecraft_version" to libs.versions.minecraft.forgeversion.get().split("-")[0],
                "forge_version" to libs.versions.minecraft.forgeversion.get().split("-")[1],
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
        .resolve("forge")
        .resolve("mods")
        .takeIf(File::exists)
        ?: File(rootDir, "jars")
    dependencies {
        // Dependencies
        exclude(dependency("org.jetbrains:annotations"))
        // Root
//        exclude("kotlin/**") // Use kotlin-neoforge or kotlin-forge
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
        exclude("org/apache/batik/**")
        exclude("org/apache/xmlgraphics/**")
        exclude("org/apache/xmlcommons/**")
        exclude("org/eclipse/**")
        exclude("jdk/xml/**")
        exclude("org/w3c/**")
        exclude("tomp2p/opuswrapper/**")
        exclude("org/slf4j/**")
        exclude("javax/xml/**")
        exclude("org/xml/**")
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
//        exclude("META-INF/versions/**") // Don't remove in Forge
    }

    // Minimize: remove unreachable classes. Protect libraries that use reflection/SPI/dynamic loading.
    minimize {
        // JDBC driver loaded via ServiceLoader at runtime
        exclude(dependency("com.h2database:h2"))
        // Jackson deserializes Telegram API objects via reflection
        exclude(dependency("com.fasterxml.jackson.core:jackson-databind"))
        exclude(dependency("com.fasterxml.jackson.core:jackson-core"))
        exclude(dependency("com.fasterxml.jackson.core:jackson-annotations"))
        exclude(dependency("com.fasterxml.jackson.datatype:jackson-datatype-jsr310"))
        // Telegram bots: all API method/object classes referenced by Jackson at runtime
        exclude(dependency("org.telegram:telegrambots-meta"))
        exclude(dependency("org.telegram:telegrambots-client"))
        exclude(dependency("org.telegram:telegrambots-longpolling"))
        exclude(dependency("org.telegram:telegrambots-extensions"))
        exclude(dependency("org.telegram:telegrambots-webhook"))
        // JDA: event dispatch and guild/message models referenced at runtime
        exclude(dependency("net.dv8tion:JDA"))
        // Exposed ORM: uses kotlin-reflect to map Kotlin properties to columns
        exclude(dependency("org.jetbrains.exposed:exposed-core"))
        exclude(dependency("org.jetbrains.exposed:exposed-dao"))
        exclude(dependency("org.jetbrains.exposed:exposed-jdbc"))
        exclude(dependency("org.jetbrains.exposed:exposed-java-time"))
        // Kotlin runtime: stdlib and reflect are loaded by the JVM and Exposed
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-serialization-core"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-serialization-json"))
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
//        "javax.xml", // Is present
        "kotlinx",
        "net.dv8tion",
        "net.kyori",
        "net.thauvin",
        "okhttp3",
        "okio",
        "org.apache",
        "org.h2",
//        "org.jetbrains.exposed", // Don't relocate
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlinx",
        "org.json",
//        "org.slf4j", // Is present
        "org.sqlite",
        "org.telegram",
        "org.w3c.css",
        "org.w3c.dom",
//        "org.xml.sax", // Is present
        "ru.astrainteractive.astralibs",
        "ru.astrainteractive.klibs",
    ).forEach { pattern -> relocate(pattern, "${requireProjectInfo.group}.shade.$pattern") }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(requireJinfo.jtarget.majorVersion)

minecraft {
    mappings("official", "1.20.1")
    useDefaultAccessTransformer()
}

dependencies {
    compileOnly(minecraft.dependency(libs.minecraft.forgeversion.get()))
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
}

renamer {
    mappings.from(minecraft.dependency.toSrgFile)
}

val reobfShadowJar by renamer.classes(tasks.named<Jar>("shadowJar")) {
    output = input
}

shadowJar.finalizedBy(reobfShadowJar)
reobfShadowJar.mustRunAfter(shadowJar)
