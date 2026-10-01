package ru.astrainteractive.messagebridge.core.di

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.config.YamlConfigFile
import java.io.File

class CoreModule(
    val dataFolder: File,
    val dispatchers: KotlinDispatchers,
    val platformServer: PlatformServer,
    val multiplatformCommand: MultiplatformCommand,
    commandRegistrarContextFactory: (mainScope: CoroutineScope) -> CommandRegistrarContext
) {
    private fun createCoroutineExceptionHandler() = CoroutineExceptionHandler { _, throwable ->
        val logger = JUtiltLogger("CoroutineExceptionHandler-AspeKt")
        logger.error(throwable) { "Error happened inside global coroutine scope!" }
    }

    val ioScope = CoroutineFeature
        .Default(dispatchers.IO + SupervisorJob() + createCoroutineExceptionHandler())
        .withTimings()

    val mainScope: CoroutineScope by lazy {
        CoroutineFeature
            .Default(dispatchers.Main + SupervisorJob() + createCoroutineExceptionHandler())
            .withTimings()
    }

    val unconfinedScope = CoroutineFeature
        .Default(dispatchers.Unconfined + SupervisorJob() + createCoroutineExceptionHandler())
        .withTimings()
    val commandRegistrarContext = commandRegistrarContextFactory.invoke(unconfinedScope)

    val configuration: YamlConfiguration = Yaml.default.configuration.copy(
        encodeDefaults = true,
        strictMode = false
    )
    val yaml: Yaml = Yaml(
        serializersModule = Yaml.default.serializersModule,
        configuration = configuration
    )
    val yamlStringFormat = yaml

    val configFile = YamlConfigFile(
        stringFormat = yamlStringFormat,
        serializer = PluginConfiguration.serializer(),
        file = dataFolder.resolve("config.yml"),
        factory = ::PluginConfiguration
    )

    val configKrate = configFile.krate()

    val translationFile = YamlConfigFile(
        stringFormat = yamlStringFormat,
        serializer = PluginTranslation.serializer(),
        file = dataFolder.resolve("translations.yml"),
        factory = ::PluginTranslation
    )

    val translationKrate = translationFile.krate()

    val commandExceptionHandler = CommandExceptionHandler(
        multiplatformCommand = multiplatformCommand,
        translationKrate = translationKrate
    )

    val lifecycle = Lifecycle.Lambda(
        onReload = {
            configKrate.getValue()
            translationKrate.getValue()
        },
        onDisable = {
            ioScope.cancel()
            mainScope.cancel()
            unconfinedScope.cancel()
        }
    )
}
