package ru.astrainteractive.messagebridge.core.di

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.serialization.StringFormat
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.StateFlowMutableKrate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.factory.ConfigKrateFactory
import java.io.File

interface CoreGraph {
    val dispatchers: KotlinDispatchers
    val dataFolder: File
    val luckPermsProvider: LuckPermsProvider
    val onlinePlayersProvider: OnlinePlayersProvider

    @Provides
    fun provideYamlConfig() = Yaml.default.configuration.copy(
        encodeDefaults = true,
        strictMode = false
    )

    @Provides
    fun provideYaml(configuration: YamlConfiguration): StringFormat = Yaml(
        serializersModule = Yaml.default.serializersModule,
        configuration = configuration
    )

    @Provides
    fun provideScope(dispatchers: KotlinDispatchers) = CoroutineFeature.Default(dispatchers.IO)


    @Provides
    fun provideConfigKrate(yamlStringFormat: StringFormat) = ConfigKrateFactory.create(
        fileNameWithoutExtension = "config",
        stringFormat = yamlStringFormat,
        dataFolder = dataFolder,
        factory = ::PluginConfiguration
    )

    @Provides
    fun provideTranslationKrate(yamlStringFormat: StringFormat) = ConfigKrateFactory.create(
        fileNameWithoutExtension = "translations",
        stringFormat = yamlStringFormat,
        dataFolder = dataFolder,
        factory = ::PluginTranslation
    )

    @Provides
    fun provideLifecycle(
        configKrate: StateFlowMutableKrate<PluginConfiguration>,
        translationKrate: StateFlowMutableKrate<PluginTranslation>,
        scope: CoroutineScope
    ) = Lifecycle.Lambda(
        onReload = {
            configKrate.getValue()
            translationKrate.getValue()
        },
        onDisable = {
            scope.cancel()
        }
    )
}