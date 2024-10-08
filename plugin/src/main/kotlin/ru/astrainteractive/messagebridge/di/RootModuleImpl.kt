package ru.astrainteractive.messagebridge.di

import CommandManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.async.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.serialization.YamlStringFormat
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.bot.TelegramBot
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.di.factory.ConfigKrateFactory
import ru.astrainteractive.messagebridge.events.BukkitEvent
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.TelegramMessageController
import kotlin.time.Duration.Companion.seconds

class RootModuleImpl(
    private val plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
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

    val minecraftMessageController = MinecraftMessageController()

    val bridgeBotFlow = configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, TelegramBot>(
            scope = scope,
            transform = { config, prev ->
                info { "#bridgeBotFlow closing previous bot" }
                prev?.clearWebhook()
                prev?.onClosing()
                info { "#bridgeBotFlow loading bot" }
                val bot = TelegramBot(
                    configKrate = configKrate,
                    translationKrate = translationKrate,
                    minecraftMessageController = minecraftMessageController,
                    scope = scope,
                    dispatchers = dispatchers
                )
                info { "#bot loaded!" }
                bot
            }
        )

    val telegramMessageController = TelegramMessageController(
        configKrate = configKrate,
        tgBotFlow = bridgeBotFlow
    )

    private val bukkitEvent = BukkitEvent(
        configKrate = configKrate,
        translationKrate = translationKrate,
        telegramMessageController = telegramMessageController,
        scope = scope,
        dispatchers = dispatchers
    )

    private val commandManager = CommandManager(
        translationKrate = translationKrate,
        kyoriKrate = kyoriKrate,
        plugin = plugin
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            commandManager
            bukkitEvent.onEnable(plugin)
        },
        onReload = {
            configKrate.loadAndGet()
            translationKrate.loadAndGet()
        },
        onDisable = {
            bukkitEvent.onDisable()
            HandlerList.unregisterAll(plugin)
            scope.cancel()
            runBlocking(dispatchers.IO) {
                runCatching {
                    bridgeBotFlow.timeout(TIMEOUT).first().let { bot ->
                        bot.clearWebhook()
                        bot.onClosing()
                    }
                }.onFailure { error { "#onDisable could not close TGBot: ${it.message} ${it.cause?.message}" } }
            }
        }
    )

    companion object {
        private val TIMEOUT = 5.seconds
    }
}
