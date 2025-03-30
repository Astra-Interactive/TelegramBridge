pluginManagement {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        maven("https://jitpack.io")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        google()
        maven("https://maven.minecraftforge.net")
    }
}

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven("https://repo.glaremasters.me/repository/towny/")
        maven("https://nexus.scarsz.me/content/groups/public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.essentialsx.net/snapshots/")
        maven("https://repo.essentialsx.net/releases/")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://maven.enginehub.org/repo/")
        maven("https://m2.dv8tion.net/releases")
        maven("https://repo1.maven.org/maven2/")
        maven("https://maven.playpro.com")
        maven("https://jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "MessageBridge"

// Spigot
include(":instances:bukkit")
include(":instances:forge")
include(":modules:messenger:api")
include(":modules:messenger:bukkit")
include(":modules:messenger:forge")
include(":modules:messenger:telegram")
include(":modules:messenger:discord")
include(":modules:bridge")
include(":modules:core:api")
include(":modules:core:bukkit")
include(":modules:core:forge")
include(":modules:link")
