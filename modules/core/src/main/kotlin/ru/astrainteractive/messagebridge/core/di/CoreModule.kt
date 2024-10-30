package ru.astrainteractive.messagebridge.core.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.serialization.YamlStringFormat
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.di.factory.ConfigKrateFactory
import java.io.File

class CoreModule(
    val dataFolder: File,
    val dispatchers: KotlinDispatchers
) {
    val yamlStringFormat = YamlStringFormat()

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

    val kyoriKrate = DefaultMutableKrate<KyoriComponentSerializer>(
        factory = { KyoriComponentSerializer.Legacy },
        loader = { null }
    )

    val lifecycle = Lifecycle.Lambda(
        onReload = {
            configKrate.loadAndGet()
            translationKrate.loadAndGet()
        },
        onDisable = {
            scope.cancel()
        }
    )
}
