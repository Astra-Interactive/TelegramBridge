package ru.astrainteractive.messagebridge.messenger.telegram.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramBEventConsumer

class TelegramMessengerModule(
    coreModule: CoreModule,
    onlinePlayersProvider: OnlinePlayersProvider,
    linkModule: LinkModule,
) : Logger by JUtiltLogger("MessageBridge-TelegramModule") {

    private val telegramClientFlow = coreModule.configKrate.cachedStateFlow
        .mapCached<PluginConfiguration, OkHttpTelegramClient>(
            scope = coreModule.scope,
            transform = { config, _ ->
                val tgConfig = config.tgConfig
                val client = OkHttpTelegramClient(tgConfig.token)
                info { "#telegramClientFlow telegram client created!" }
                client
            }
        )

    private val telegramMessageController = TelegramBEventConsumer(
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        telegramClientFlow = telegramClientFlow,
        dispatchers = coreModule.dispatchers
    )

    private val consumer = TelegramChatConsumer(
        configKrate = coreModule.configKrate,
        telegramClientFlow = telegramClientFlow,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
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
            telegramMessageController.cancel()
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
