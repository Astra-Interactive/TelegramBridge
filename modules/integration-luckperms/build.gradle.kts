plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.klibs.rules.gradle.linter)
    id("ru.astrainteractive.gradleplugin.java.version")
    id("java-test-fixtures")
}

linter {
    unitKind.set("technology")
    rootPackage.set("ru.astrainteractive.messagebridge.integration.luckperms")
}

dependencies {
    compileOnly(libs.minecraft.luckperms)

    implementation(libs.klibs.kstorage)
    implementation(libs.klibs.mikro.core)
    implementation(libs.minecraft.astralibs.core)

    implementation(projects.modules.core.api)

    testImplementation(libs.minecraft.luckperms)
    testImplementation(libs.tests.kotlin.test)
}
