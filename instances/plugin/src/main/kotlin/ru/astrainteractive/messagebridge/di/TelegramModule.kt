package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.discordbot.module.bridge.di.ClientBridgeModule
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.events.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.TelegramMessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import kotlin.time.Duration.Companion.seconds

class TelegramModule(
    coreModule: CoreModule,
    clientBridgeModule: ClientBridgeModule
) : Logger by JUtiltLogger("MessageBridge-TelegramModule") {
    val telegramClientFlow = coreModule.configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, OkHttpTelegramClient>(
            scope = coreModule.scope,
            transform = { config, prev ->
                info { "#telegramClientFlow creating telegram client ${config.token}_token" }
                val clinet = OkHttpTelegramClient(config.token)
                info { "#telegramClientFlow telegram client created!" }
                clinet
            }
        )

    val telegramMessageController = TelegramMessageController(
        configKrate = coreModule.configKrate,
        telegramClientFlow = telegramClientFlow,
        translationKrate = coreModule.translationKrate
    )

    val minecraftMessageController = MinecraftMessageController(
        kyoriKrate = coreModule.kyoriKrate,
        translationKrate = coreModule.translationKrate
    )

    val consumer = TelegramChatConsumer(
        configKrate = coreModule.configKrate,
        telegramClientFlow = telegramClientFlow,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        clientBridgeApi = clientBridgeModule.clientBridgeApi,
        minecraftMessageController = minecraftMessageController
    )

    val bridgeBotFlow = coreModule.configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, TelegramBotsLongPollingApplication>(
            scope = coreModule.scope,
            transform = { config, prev ->

                info { "#bridgeBotFlow closing previous bot ${config.token}" }
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

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.scope.launch { telegramMessageController.send(ServerEvent.ServerOpen) }
        },
        onDisable = {
            coreModule.scope.launch { telegramMessageController.send(ServerEvent.ServerOpen) }
            runBlocking(coreModule.dispatchers.IO) {
                runCatching {
                    bridgeBotFlow.timeout(TIMEOUT).first().let { bot ->
                        bot.unregisterBot(coreModule.configKrate.cachedStateFlow.value.token)
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
