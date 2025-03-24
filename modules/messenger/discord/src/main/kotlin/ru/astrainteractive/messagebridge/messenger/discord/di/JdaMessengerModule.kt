package ru.astrainteractive.messagebridge.messenger.discord.di

import club.minnced.discord.webhook.WebhookClient
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.Credentials
import okhttp3.OkHttpClient
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordBEventConsumer
import java.net.InetSocketAddress
import java.net.Proxy

class JdaMessengerModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    onlinePlayersProvider: OnlinePlayersProvider
) {

    private val jdaFlow = coreModule.configKrate.cachedStateFlow
        .map { it.jdaConfig }
        .distinctUntilChanged()
        .mapCached(coreModule.scope) { config, old: JDA? ->
            old?.let { jda ->
                jda.shutdownNow()
                jda.awaitShutdown()
                jda.registeredListeners.forEach(jda::removeEventListener)
            }

            JDABuilder.createLight(config.token).apply {
                enableIntents(GatewayIntent.MESSAGE_CONTENT)
                enableIntents(GatewayIntent.DIRECT_MESSAGES)
                enableIntents(GatewayIntent.GUILD_MESSAGES)
                setActivity(Activity.playing(config.activity))
                config.proxy?.let { proxy ->
                    val credential: String = Credentials.basic(proxy.username, proxy.password)
                    setWebsocketFactory(
                        WebSocketFactory()
                            .setVerifyHostname(false)
                            .also {
//                                it.setSocketTimeout(10000)
//                                it.setConnectionTimeout(10000)
                                it.proxySettings.setHost(proxy.host)
                                it.proxySettings.setPort(proxy.port)
                                it.proxySettings.setCredentials(proxy.username, proxy.password)
                            }
                    )
                    setHttpClientBuilder(
                        OkHttpClient.Builder()
                            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
                            .proxyAuthenticator { route, response ->
                                var builder = response.request.newBuilder()
                                if (route?.socketAddress?.hostString == proxy.host) {
                                    builder = builder.header("Proxy-Authorization", credential)
                                }
                                builder.build()
                            }
                    )
                }
            }.build().awaitReady()
        }

    private val webhookClient = jdaFlow.mapCached<JDA, WebhookClient>(coreModule.scope) { jda, old ->
        old?.close()
        val channel = coreModule.configKrate.cachedValue.jdaConfig.channelId
        WebHookClientFactory(jda).create(channel).first()
    }

    private val discordMessageController = DiscordBEventConsumer(
        jdaFlow = jdaFlow,
        webHookClientFlow = webhookClient,
        translationKrate = coreModule.translationKrate,
        configKrate = coreModule.configKrate,
        linkingDao = linkModule.linkingDao,
        onlinePlayersProvider = onlinePlayersProvider,
        dispatchers = coreModule.dispatchers
    )

    private val messageEventListener = MessageEventListener(
        configKrate = coreModule.configKrate,
        onlinePlayersProvider = onlinePlayersProvider,
        translationKrate = coreModule.translationKrate,
        linkApi = linkModule.linkApi
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            jdaFlow
                .onEach { messageEventListener.onEnable(it) }
                .launchIn(coreModule.scope)
        },
        onDisable = {
            discordMessageController.cancel()
            messageEventListener.cancel()
            GlobalScope.launch {
                jdaFlow.firstOrNull()?.let { jda ->
                    messageEventListener.onDisable(jda)
                    jda.registeredListeners.forEach(jda::removeEventListener)
                    jda.shutdownNow()
                    jda.awaitShutdown()
                }
            }
        }
    )
}
