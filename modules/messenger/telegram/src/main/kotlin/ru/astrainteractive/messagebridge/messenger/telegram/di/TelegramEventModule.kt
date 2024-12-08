package ru.astrainteractive.messagebridge.messenger.telegram.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramChatConsumer

class TelegramEventModule(
    coreModule: CoreModule,
    onlinePlayersProvider: OnlinePlayersProvider,
    linkModule: LinkModule,
    coreTelegramModule: CoreTelegramModule,
    minecraftMessageController: MessageController,
    discordMessageController: MessageController,
) : Logger by JUtiltLogger("MessageBridge-TelegramModule") {
    private val consumer = TelegramChatConsumer(
        configKrate = coreModule.configKrate,
        telegramClientFlow = coreTelegramModule.telegramClientFlow,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        minecraftMessageController = minecraftMessageController,
        discordMessageController = discordMessageController,
        onlinePlayersProvider = onlinePlayersProvider,
        translationKrate = coreModule.translationKrate,
        linkApi = linkModule.linkApi
    )

    private val bridgeBotFlow = coreModule.configKrate.cachedStateFlow
        .map { it.tgConfig }
        .distinctUntilChanged()
        .mapCached<PluginConfiguration.TelegramConfig, TelegramBotsLongPollingApplication>(
            scope = coreModule.scope,
            transform = { tgConfig, prev ->
                info { "#bridgeBotFlow closing previous bot ${tgConfig.token}" }
                prev?.unregisterBot(tgConfig.token)
                prev?.stop()
                prev?.close()
                info { "#bridgeBotFlow loading bot" }

                val longPollingApplication = TelegramBotsLongPollingApplication()
                try {
                    longPollingApplication.registerBot(tgConfig.token, consumer)
                    info { "#bot loaded!" }
                } catch (e: TelegramApiErrorResponseException) {
                    info { "#telegramMessageListener could not load event. Error ${e.message}" }
                }
                longPollingApplication
            }
        )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            GlobalScope.launch {
                runCatching {
                    bridgeBotFlow.firstOrNull()?.let { bot ->
                        bot.unregisterBot(coreModule.configKrate.cachedStateFlow.value.tgConfig.token)
                        bot.stop()
                        bot.close()
                    }
                }.onFailure { error { "#onDisable could not close TGBot: ${it.message} ${it.cause?.message}" } }
            }
        }
    )
}
