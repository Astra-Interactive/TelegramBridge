package ru.astrainteractive.messagebridge.core.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.async.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.serialization.YamlStringFormat
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.di.factory.ConfigKrateFactory

class CoreModule(
    val plugin: LifecyclePlugin,
) {
    val yamlStringFormat = YamlStringFormat()

    val dispatchers = DefaultBukkitDispatchers(plugin)

    val scope = CoroutineFeature.Default(dispatchers.IO)

    val configKrate = ConfigKrateFactory.create(
        fileNameWithoutExtension = "config",
        stringFormat = yamlStringFormat,
        dataFolder = plugin.dataFolder,
        factory = ::PluginConfiguration
    )

    val translationKrate = ConfigKrateFactory.create(
        fileNameWithoutExtension = "translations",
        stringFormat = yamlStringFormat,
        dataFolder = plugin.dataFolder,
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
