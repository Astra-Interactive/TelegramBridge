package ru.astrainteractive.messagebridge.core.api.di

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.util.krateOf
import ru.astrainteractive.klibs.kstorage.api.asStateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.asStateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.config.describeConfigError
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

    private val configLogger = JUtiltLogger("MessageBridge-config")

    val configKrate = yamlStringFormat
        .krateOf(
            file = dataFolder.resolve("config.yml"),
            factory = ::PluginConfiguration
        )
        .asStateFlowMutableKrate()

    val config: StateFlow<PluginConfiguration> = configKrate.cachedStateFlow
        .mapNotNull { result -> result.getOrNull() }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = configKrate.cachedValue.getOrElse { _ -> PluginConfiguration() }
        )

    private val translationLogger = JUtiltLogger("MessageBridge-translations")

    private val translationFileKrate = yamlStringFormat.krateOf(
        file = dataFolder.resolve("translations.yml"),
        factory = ::PluginTranslation
    )

    val translationKrate = DefaultMutableKrate(
        factory = ::PluginTranslation,
        loader = {
            translationFileKrate.getValue()
                .onFailure { error ->
                    translationLogger.error {
                        "translations.yml has an error, the default texts are used: ${describeConfigError(error)}"
                    }
                }
                .getOrNull()
        }
    ).asStateFlowKrate()

    val commandExceptionHandler = CommandExceptionHandler(
        multiplatformCommand = multiplatformCommand,
        translationKrate = translationKrate
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            logConfigError(configKrate.cachedValue)
        },
        onReload = {
            logConfigError(configKrate.getValue())
            translationKrate.getValue()
        },
        onDisable = {
            ioScope.cancel()
            mainScope.cancel()
            unconfinedScope.cancel()
        }
    )

    private fun logConfigError(result: Result<PluginConfiguration>) {
        result.onFailure { error ->
            configLogger.error {
                "config.yml has an error and is not applied, the previous settings are kept: " +
                    describeConfigError(error)
            }
        }
    }
}
