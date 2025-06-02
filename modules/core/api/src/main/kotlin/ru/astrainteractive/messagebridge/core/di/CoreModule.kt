package ru.astrainteractive.messagebridge.core.di

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.di.factory.ConfigKrateFactory
import java.io.File

class CoreModule(
    val dataFolder: File,
    val dispatchers: KotlinDispatchers
) {
    val configuration: YamlConfiguration = Yaml.default.configuration.copy(
        encodeDefaults = true,
        strictMode = false
    )
    val yaml: Yaml = Yaml(
        serializersModule = Yaml.default.serializersModule,
        configuration = configuration
    )
    val yamlStringFormat = yaml

    val scope = CoroutineFeature.Default(dispatchers.IO)

    val configKrate = ConfigKrateFactory.create(
        fileNameWithoutExtension = "config",
        stringFormat = yamlStringFormat,
        dataFolder = dataFolder,
        factory = ::PluginConfiguration
    )

    val translationKrate = ConfigKrateFactory.create(
        fileNameWithoutExtension = "translations",
        stringFormat = yamlStringFormat,
        dataFolder = dataFolder,
        factory = ::PluginTranslation
    )

    val lifecycle = Lifecycle.Lambda(
        onReload = {
            configKrate.getValue()
            translationKrate.getValue()
        },
        onDisable = {
            scope.cancel()
        }
    )
}
