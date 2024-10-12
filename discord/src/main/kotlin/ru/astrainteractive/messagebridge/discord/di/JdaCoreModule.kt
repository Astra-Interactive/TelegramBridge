package ru.astrainteractive.messagebridge.discord.di

import ru.astrainteractive.astralibs.serialization.YamlStringFormat
import ru.astrainteractive.messagebridge.core.di.factory.ConfigKrateFactory
import ru.astrainteractive.messagebridge.discord.core.JdaConfig
import java.io.File

class JdaCoreModule {
    val yamlStringFormat = YamlStringFormat()
    val config = ConfigKrateFactory.create(
        fileNameWithoutExtension = "confg",
        stringFormat = yamlStringFormat,
        dataFolder = File("./"),
        factory = ::JdaConfig
    )
}
