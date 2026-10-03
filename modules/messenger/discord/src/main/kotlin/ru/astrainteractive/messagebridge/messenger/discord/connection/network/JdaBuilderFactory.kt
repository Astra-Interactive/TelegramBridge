package ru.astrainteractive.messagebridge.messenger.discord.connection.network

import com.neovisionaries.ws.client.WebSocketFactory
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import okhttp3.Credentials
import okhttp3.OkHttpClient
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.DiscordConnectionSettings
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class JdaBuilderFactory {

    private fun createWebSocketFactory(proxy: PluginConfiguration.Proxy): WebSocketFactory {
        return WebSocketFactory()
            .setVerifyHostname(false)
            .also { webSocketFactory ->
                webSocketFactory.proxySettings.setHost(proxy.host)
                webSocketFactory.proxySettings.setPort(proxy.port)
                proxy.credentials?.let { (username, password) ->
                    webSocketFactory.proxySettings.setCredentials(username, password)
                }
            }
    }

    fun create(settings: DiscordConnectionSettings, okHttpClient: OkHttpClient?): JDABuilder {
        val config = settings.jdaConfig
        return JDABuilder.createLight(config.token).apply {
            enableIntents(settings.intents)
            config.activity.takeIf(String::isNotBlank)?.let { activity -> setActivity(Activity.playing(activity)) }
            setMaxReconnectDelay(MAX_RECONNECT_DELAY.inWholeSeconds.toInt())
            config.proxy?.let { proxy -> setWebsocketFactory(createWebSocketFactory(proxy)) }
            okHttpClient?.let(::setHttpClient)
        }
    }

    fun createOkHttpClient(proxy: PluginConfiguration.Proxy): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .writeTimeout(WRITE_TIMEOUT.toJavaDuration())
            .readTimeout(READ_TIMEOUT.toJavaDuration())
            .callTimeout(CALL_TIMEOUT.toJavaDuration())
            .pingInterval(PING_INTERVAL.toJavaDuration())
            .retryOnConnectionFailure(true)
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
        proxy.credentials?.let { (username, password) ->
            val credential = Credentials.basic(username, password)
            builder.proxyAuthenticator { _, response ->
                if (response.request.header(PROXY_AUTHORIZATION) != null) return@proxyAuthenticator null
                response.request.newBuilder()
                    .header(PROXY_AUTHORIZATION, credential)
                    .build()
            }
        }
        return builder.build()
    }

    private companion object {
        const val PROXY_AUTHORIZATION = "Proxy-Authorization"
        val MAX_RECONNECT_DELAY: Duration = 32.seconds
        val CONNECT_TIMEOUT: Duration = 10.seconds
        val WRITE_TIMEOUT: Duration = 10.seconds
        val READ_TIMEOUT: Duration = 60.seconds
        val CALL_TIMEOUT: Duration = 75.seconds
        val PING_INTERVAL: Duration = 15.seconds
    }
}
