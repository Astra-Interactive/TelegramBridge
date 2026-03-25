import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.named
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.klibs.minecraft.resource.processor)
    alias(libs.plugins.gradle.shadow)
}

dependencies {
    compileOnly(libs.driver.h2)
    compileOnly(libs.driver.jdbc)
    compileOnly(libs.driver.mysql)
    compileOnly(libs.minecraft.discordsrv)
    compileOnly(libs.minecraft.essentialsx)
    compileOnly(libs.minecraft.luckperms)
    compileOnly(libs.minecraft.paper.api)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.datetime)
    implementation(libs.minecraft.astralibs.command)
    implementation(libs.minecraft.astralibs.command.bukkit)
    implementation(libs.minecraft.astralibs.core)
    implementation(libs.minecraft.astralibs.core.bukkit)
    implementation(libs.minecraft.astralibs.menu.bukkit)
    implementation(libs.minecraft.bstats)
    implementation(libs.minecraft.vaultapi)

    implementation(projects.modules.command)
    implementation(projects.modules.core.api)
    implementation(projects.modules.core.bukkit)
    implementation(projects.modules.link)
    implementation(projects.modules.messenger.api)
    implementation(projects.modules.messenger.bukkit)
    implementation(projects.modules.messenger.discord)
    implementation(projects.modules.messenger.telegram)
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

val shadowJar = tasks.named<ShadowJar>("shadowJar")
shadowJar.configure {

    val projectInfo = requireProjectInfo
    isReproducibleFileOrder = true
    mergeServiceFiles()
    dependsOn(configurations)
    archiveClassifier.set(null as String?)

    minimize {
        exclude(dependency(libs.exposed.jdbc.get()))
        exclude(dependency(libs.exposed.dao.get()))
    }
    archiveVersion.set(projectInfo.versionString)
    archiveBaseName = "${requireProjectInfo.name}-${project.name}"
    destinationDirectory = rootDir.resolve("build")
        .resolve("bukkit")
        .resolve("plugins")
        .takeIf(File::exists)
        ?: File(rootDir, "jars").also(File::mkdirs)

    dependencies {
        // Dependencies
        exclude("ch/qos/logback/**")
        exclude("com/ibm/icu/**")
        exclude("it/unimi/dsi/**")
        exclude("javax/**")
        exclude("mozilla/**")
        exclude("org/apache/batik/**")
        exclude("org/apache/commons/logging/**")
        exclude("org/apache/xmlgraphics/**")
        exclude("org/intellij/lang/annotations/**")
        exclude("org/jetbrains/annotations/**")
        exclude("org/slf4j/**")
        exclude("org/w3c/dom/**")
        // Root
        exclude("**LICENCE**")
        exclude("**LICENSE**")
        exclude("_COROUTINE/**")
        exclude("DebugProbesKt.bin")
        exclude("jetty-dir.css")
        exclude("LICENSE")
        exclude("license/**")
        exclude("licenses/**")
        exclude("natives/**")
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
        // DEPENDENCIES
        exclude(dependency("com.fasterxml.jackson.core:.*"))
        exclude(dependency("com.google.code.gson:.*"))
        exclude(dependency("com.google.crypto.tink:.*"))
        exclude(dependency("com.google.errorprone:.*"))
        exclude(dependency("com.mojang:brigadier"))
        exclude(dependency("com.mysql:mysql-connector-j"))
        exclude(dependency("mysql:mysql-connector-java"))
        exclude(dependency("net.java.dev.jna:.*"))
        exclude(dependency("net.kyori:.*"))
        exclude(dependency("org.apache.xmlgraphics:.*"))
        exclude(dependency("org.bouncycastle:.*"))
        exclude(dependency("org.checkerframework:.*"))
        exclude(dependency("org.conscrypt:.*"))
        exclude(dependency("org.eclipse.jetty.toolchain:.*"))
        exclude(dependency("org.eclipse.jetty:.*"))
        exclude(dependency("org.xerial:sqlite-jdbc"))
    }
    relocate("org.bstats", projectInfo.group)
    listOf(
        "ch.qos.logback",
        "club.minnced.discord",
        "club.minnced.opus",
        "co.touchlab.stately",
        "com.charleskorn.kaml",
        "com.ibm.icu",
        "com.neovisionaries.ws",
        "gnu.trove",
        "google.protobuf",
        "io.github.reactivecircus",
        "it.krzeminski.snakeyaml",
        "net.dv8tion",
        "net.thauvin.erik",
        "okhttp3",
        "okio",
        "org.apache",
        "org.intellij",
        "org.jetbrains.annotations",
        "org.json",
        "org.telegram.telegrambots",
        "ru.astrainteractive.astralibs",
        "ru.astrainteractive.klibs",
        "tomp2p.opuswrapper",
    ).forEach { pattern -> relocate(pattern, "${projectInfo.group}.$pattern") }
    listOf(
        "kotlinx",
    ).forEach { pattern ->
        relocate(pattern, "${projectInfo.group}.$pattern") {
            exclude("kotlin/kotlin.kotlin_builtins")
        }
    }
}
