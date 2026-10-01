package ru.astrainteractive.messagebridge.core.api.config

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.io.File

class TranslationFolder(dataFolder: File) : Logger by JUtiltLogger("MessageBridge-TranslationFolder") {
    private val legacyMainFile: File = dataFolder.resolve(LEGACY_MAIN_FILE)

    val directory: File = dataFolder.resolve(DIRECTORY)

    val mainFile: File = directory.resolve(MAIN_FILE)

    fun prepare() {
        directory.mkdirs()
        if (!legacyMainFile.exists() || mainFile.exists()) return
        if (legacyMainFile.renameTo(mainFile)) {
            info { "#prepare $LEGACY_MAIN_FILE is moved to $DIRECTORY/$MAIN_FILE" }
        } else {
            error { "#prepare could not move $LEGACY_MAIN_FILE to $DIRECTORY/$MAIN_FILE, the default texts are used" }
        }
    }

    private companion object {
        const val DIRECTORY = "translation"
        const val MAIN_FILE = "main.yml"
        const val LEGACY_MAIN_FILE = "translations.yml"
    }
}
