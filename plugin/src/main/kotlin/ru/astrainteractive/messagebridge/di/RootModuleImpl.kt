package ru.astrainteractive.messagebridge.di

import CommandManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import org.bukkit.event.HandlerList
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.async.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.serialization.YamlStringFormat
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.discordbot.module.bridge.di.BridgeModule
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.di.factory.ConfigKrateFactory
import ru.astrainteractive.messagebridge.events.BridgeEvent
import ru.astrainteractive.messagebridge.events.BukkitEvent
import ru.astrainteractive.messagebridge.events.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.TelegramMessageController
import kotlin.time.Duration.Companion.seconds

class RootModuleImpl(
    private val plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    val bridgeModule = BridgeModule.Default()

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

    val minecraftMessageController = MinecraftMessageController(
        kyoriKrate = kyoriKrate,
        translationKrate = translationKrate
    )

    val telegramClientFlow = configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, OkHttpTelegramClient>(
            scope = scope,
            transform = { config, prev ->
                info { "#telegramClientFlow creating telegram client" }
                val clinet = OkHttpTelegramClient(config.token)
                info { "#telegramClientFlow telegram client created!" }
                clinet
            }
        )

    val consumer = TelegramChatConsumer(
        configKrate = configKrate,
        minecraftMessageController = minecraftMessageController,
        telegramClientFlow = telegramClientFlow,
        scope = scope,
        dispatchers = dispatchers,
        clientBridgeApi = bridgeModule.clientBridgeApi
    )

    val bridgeBotFlow = configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, TelegramBotsLongPollingApplication>(
            scope = scope,
            transform = { config, prev ->

                info { "#bridgeBotFlow closing previous bot" }
                prev?.unregisterBot(config.token)
                prev?.stop()
                prev?.close()
                info { "#bridgeBotFlow loading bot" }

                val longPollingApplication = TelegramBotsLongPollingApplication()
                longPollingApplication.registerBot(config.token, consumer)
                info { "#bot loaded!" }
                longPollingApplication
            }
        )

    val telegramMessageController = TelegramMessageController(
        configKrate = configKrate,
        telegramClientFlow = telegramClientFlow,
        translationKrate = translationKrate
    )

    private val bukkitEvent = BukkitEvent(
        configKrate = configKrate,
        telegramMessageController = telegramMessageController,
        scope = scope,
        dispatchers = dispatchers,
        clientBridgeApi = bridgeModule.clientBridgeApi
    )

    private val bridgeEvent = BridgeEvent(
        clientBridgeApi = bridgeModule.clientBridgeApi,
        minecraftMessageController = minecraftMessageController,
        telegramMessageController = telegramMessageController
    )

    private val commandManager by lazy {
        CommandManager(
            translationKrate = translationKrate,
            kyoriKrate = kyoriKrate,
            plugin = plugin
        )
    }

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            commandManager
            bukkitEvent.onEnable(plugin)
            bridgeModule.lifecycle.onEnable()
        },
        onReload = {
            configKrate.loadAndGet()
            translationKrate.loadAndGet()
        },
        onDisable = {
            bukkitEvent.onDisable()
            bridgeEvent.cancel()
            bridgeModule.lifecycle.onDisable()
            HandlerList.unregisterAll(plugin)
            scope.cancel()
            runBlocking(dispatchers.IO) {
                runCatching {
                    bridgeBotFlow.timeout(TIMEOUT).first().let { bot ->
                        bot.unregisterBot(configKrate.cachedStateFlow.value.token)
                        bot.stop()
                        bot.close()
                    }
                }.onFailure { error { "#onDisable could not close TGBot: ${it.message} ${it.cause?.message}" } }
            }
        }
    )

    companion object {
        private val TIMEOUT = 5.seconds
    }
}
