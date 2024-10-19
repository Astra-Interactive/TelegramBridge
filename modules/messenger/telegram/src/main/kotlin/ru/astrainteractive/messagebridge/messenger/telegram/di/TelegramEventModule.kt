package ru.astrainteractive.messagebridge.messenger.telegram.di

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.MinecraftBridge
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramChatConsumer
import kotlin.time.Duration.Companion.seconds

class TelegramEventModule(
    coreModule: CoreModule,
    minecraftBridge: MinecraftBridge,
    coreTelegramModule: CoreTelegramModule,
    minecraftMessageController: MessageController,
    discordMessageController: MessageController
) : Logger by JUtiltLogger("MessageBridge-TelegramModule") {
    private val consumer = TelegramChatConsumer(
        configKrate = coreModule.configKrate,
        telegramClientFlow = coreTelegramModule.telegramClientFlow,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        minecraftMessageController = minecraftMessageController,
        discordMessageController = discordMessageController,
        minecraftBridge = minecraftBridge
    )

    private val bridgeBotFlow = coreModule.configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, TelegramBotsLongPollingApplication>(
            scope = coreModule.scope,
            transform = { config, prev ->
                val tgConfig = config.tgConfig

                info { "#bridgeBotFlow closing previous bot ${tgConfig.token}" }
                prev?.unregisterBot(tgConfig.token)
                prev?.stop()
                prev?.close()
                info { "#bridgeBotFlow loading bot" }

                val longPollingApplication = TelegramBotsLongPollingApplication()
                longPollingApplication.registerBot(tgConfig.token, consumer)
                info { "#bot loaded!" }
                longPollingApplication
            }
        )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            runBlocking(coreModule.dispatchers.IO) {
                runCatching {
                    bridgeBotFlow.timeout(TIMEOUT).first().let { bot ->
                        bot.unregisterBot(coreModule.configKrate.cachedStateFlow.value.tgConfig.token)
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
