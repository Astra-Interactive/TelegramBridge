package ru.astrainteractive.messagebridge.core.di

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.util.parseOrWriteIntoDefault
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.asStateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import java.io.File

class CoreModule(
    val dataFolder: File,
    val dispatchers: KotlinDispatchers,
    val platformServer: PlatformServer,
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

    val configKrate = DefaultMutableKrate(
        factory = ::PluginConfiguration,
        loader = {
            yamlStringFormat.parseOrWriteIntoDefault(
                file = dataFolder.resolve("config.yml"),
                logger = JUtiltLogger("MessageBridge-config"),
                default = ::PluginConfiguration
            )
        }
    ).asStateFlowKrate()

    val translationKrate = DefaultMutableKrate(
        factory = ::PluginTranslation,
        loader = {
            yamlStringFormat.parseOrWriteIntoDefault(
                file = dataFolder.resolve("translations.yml"),
                logger = JUtiltLogger("MessageBridge-translations"),
                default = ::PluginTranslation
            )
        }
    ).asStateFlowKrate()

    val kyoriKrate = DefaultMutableKrate<KyoriComponentSerializer>(
        factory = { KyoriComponentSerializer.Legacy },
        loader = { null }
    ).asCachedKrate()

    val lifecycle = Lifecycle.Lambda(
        onReload = {
            configKrate.getValue()
            translationKrate.getValue()
        },
        onDisable = {
            ioScope.cancel()
            mainScope.cancel()
        }
    )
}
