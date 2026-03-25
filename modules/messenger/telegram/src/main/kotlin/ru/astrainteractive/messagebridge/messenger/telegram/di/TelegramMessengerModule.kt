package ru.astrainteractive.messagebridge.messenger.telegram.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.coroutines.mapCached
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramBEventConsumer
import java.net.InetSocketAddress
import java.net.Proxy

class TelegramMessengerModule(
    coreModule: CoreModule,
    onlinePlayersProvider: OnlinePlayersProvider,
    linkModule: LinkModule,
) : Logger by JUtiltLogger("MessageBridge-TelegramModule") {

    private val okHttpClient = coreModule.configKrate.cachedStateFlow
        .map { pluginConfiguration -> pluginConfiguration.tgConfig.proxy }
        .distinctUntilChanged()
        .map { proxy ->
            if (proxy == null) {
                OkHttpClient.Builder().build()
            } else {
                OkHttpClient
                    .Builder()
                    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
                    .proxyAuthenticator { route, response ->
                        var builder = response.request.newBuilder()
                        if (route?.socketAddress?.hostString == proxy.host) {
                            val credential: String = Credentials.basic(proxy.username, proxy.password)
                            builder = builder.header("Proxy-Authorization", credential)
                        }
                        builder.build()
                    }
                    .build()
            }
        }
        .shareIn(coreModule.ioScope, SharingStarted.Lazily, 1)

    private val telegramClientFlow = coreModule.configKrate
        .cachedStateFlow
        .map { tgConfig -> tgConfig.tgConfig }
        .distinctUntilChanged()
        .combine(okHttpClient) { tgConfig, okHttpClient ->
            OkHttpTelegramClient(
                okHttpClient,
                tgConfig.token
            )
        }
        .shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    private val telegramMessageController = TelegramBEventConsumer(
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        telegramClientFlow = telegramClientFlow,
    )

    private val consumer = TelegramChatConsumer(
        configKrate = coreModule.configKrate,
        telegramClientFlow = telegramClientFlow,
        scope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        onlinePlayersProvider = onlinePlayersProvider,
        translationKrate = coreModule.translationKrate,
        linkApi = linkModule.linkApi
    )

    private val bridgeBotFlow = coreModule.configKrate.cachedStateFlow
        .map { it.tgConfig }
        .distinctUntilChanged()
        .mapCached<PluginConfiguration.TelegramConfig, TelegramBotsLongPollingApplication>(
            scope = coreModule.ioScope,
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
            GlobalScope.launch(Dispatchers.IO) {
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
