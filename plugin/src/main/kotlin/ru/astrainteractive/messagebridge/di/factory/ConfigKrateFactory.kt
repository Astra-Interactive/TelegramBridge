package ru.astrainteractive.messagebridge.di.factory

import kotlinx.serialization.StringFormat
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.serialization.StringFormatExt.parse
import ru.astrainteractive.astralibs.serialization.StringFormatExt.writeIntoFile
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultStateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.value.ValueFactory
import java.io.File

object ConfigKrateFactory : Logger by JUtiltLogger("MessageBridge-ConfigKrateFactory") {
    inline fun <reified T> create(
        fileNameWithoutExtension: String,
        stringFormat: StringFormat,
        dataFolder: File,
        factory: ValueFactory<T>
    ): DefaultStateFlowMutableKrate<T> {
        return DefaultStateFlowMutableKrate(
            factory = factory,
            loader = {
                info { "#create trying to load file $fileNameWithoutExtension" }
                if (!dataFolder.exists()) dataFolder.mkdirs()
                val file = dataFolder.resolve("$fileNameWithoutExtension.yml")
                stringFormat.parse<T>(file)
                    .onFailure {
                        val defaultFile = when {
                            !file.exists() || file.length() == 0L -> file
                            else -> dataFolder.resolve("$fileNameWithoutExtension.default.yml")
                        }
                        if (!defaultFile.exists()) defaultFile.createNewFile()
                        stringFormat.writeIntoFile(factory.create(), defaultFile)
                        error { "Could not read $fileNameWithoutExtension.yml! Loaded default. Error -> ${it.message}" }
                    }
                    .onSuccess { stringFormat.writeIntoFile(it, file) }
                    .getOrElse { factory.create() }
            }
        )
    }
}
