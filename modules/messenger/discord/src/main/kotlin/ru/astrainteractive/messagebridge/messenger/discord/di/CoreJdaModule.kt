package ru.astrainteractive.messagebridge.messenger.discord.di

import club.minnced.discord.webhook.WebhookClient
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.Credentials
import okhttp3.OkHttpClient
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageController
import java.net.InetSocketAddress
import java.net.Proxy

class CoreJdaModule(
    coreModule: CoreModule,
) {
    val jdaFlow = coreModule.configKrate.cachedStateFlow
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
    val webhookClient = jdaFlow.mapCached<JDA, WebhookClient>(coreModule.scope) { jda, old ->
        old?.close()
        val channel = coreModule.configKrate.cachedValue.jdaConfig.channelId
        WebHookClientFactory(jda).create(channel).first()
    }

    val discordMessageController = DiscordMessageController(
        jdaFlow = jdaFlow,
        webHookClientFlow = webhookClient,
        kyoriKrate = coreModule.kyoriKrate,
        translationKrate = coreModule.translationKrate,
        configKrate = coreModule.configKrate
    )

    /**
     * handled by [EventJdaModule]
     */
    val lifecycle = Lifecycle.Lambda()
}
