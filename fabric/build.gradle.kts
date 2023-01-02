import net.fabricmc.loom.task.RemapJarTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    id("fabric-loom")
    id("com.github.johnrengelman.shadow")
    id("fabric-resource-processor")
    id("basic-java")
}
dependencies {
    mappings("net.fabricmc:yarn:1.19.2+build.8:v2")
    minecraft(libs.mojangMinecraft.get())
    modImplementation(libs.fabric.kotlin.get())
    modImplementation(libs.fabric.loader.get())
    modImplementation(libs.fabric.api.get())
    // AstraLibs
    implementation(libs.astralibs.ktxCore)
    implementation(libs.xerialSqliteJdbcLib)
    implementation(project(":domain"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    dependencies {
        // Kotlin
        include(dependency(libs.kotlinGradlePlugin.get()))
        include(dependency(libs.astralibs.ktxCore.get()))
        include(dependency(libs.xerialSqliteJdbcLib.get()))
        include(dependency(":domain"))
    }
    exclude("mappings/")
    dependsOn(configurations)
    from(sourceSets.main.get().allSource)
    mergeServiceFiles()
//    minimize()
    isReproducibleFileOrder = true
    archiveClassifier.set(null as String?)
    archiveBaseName.set("AstraTemplate")
}

val remapJar = tasks.getByName<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    mustRunAfter(shadowJar)
    this.input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
    archiveBaseName.set("AstraTemplate")
    destinationDirectory.set(File(libs.versions.destinationDirectoryFabricPath.get()))
}
tasks.assemble {
    dependsOn(remapJar)
}

artifacts {
    archives(remapJar)
    shadow(shadowJar)
}