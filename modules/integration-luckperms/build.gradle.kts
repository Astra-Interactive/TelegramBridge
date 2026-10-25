plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.klibs.rules.gradle.linter)
    id("ru.astrainteractive.gradleplugin.java.version")
    id("java-test-fixtures")
}

linter {
    unitKind.set("technology")
}

configurations.api {
    dependencies.removeIf { dependency ->
        dependency.group == "org.jetbrains.kotlin" && dependency.name == "kotlin-stdlib"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(libs.minecraft.luckperms)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.core)

    implementation(projects.modules.core.api)

    testImplementation(libs.minecraft.luckperms)
    testImplementation(libs.tests.kotlin.test)
}
