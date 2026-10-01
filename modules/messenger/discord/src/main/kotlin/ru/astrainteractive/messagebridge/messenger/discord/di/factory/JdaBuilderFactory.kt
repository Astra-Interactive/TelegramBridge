package ru.astrainteractive.messagebridge.messenger.discord.di.factory

import com.neovisionaries.ws.client.WebSocketFactory
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.Credentials
import okhttp3.OkHttpClient
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class JdaBuilderFactory {

    fun create(config: PluginConfiguration.JdaConfig, okHttpClient: OkHttpClient?): JDABuilder {
        return JDABuilder.createLight(config.token).apply {
            enableIntents(GatewayIntent.MESSAGE_CONTENT)
            enableIntents(GatewayIntent.DIRECT_MESSAGES)
            enableIntents(GatewayIntent.GUILD_MESSAGES)
            // Activity.playing throws on a blank name, and a new config has no activity
            config.activity.takeIf(String::isNotBlank)?.let { activity -> setActivity(Activity.playing(activity)) }
            setMaxReconnectDelay(MAX_RECONNECT_DELAY.inWholeSeconds.toInt())
            config.proxy?.let { proxy -> setWebsocketFactory(createWebSocketFactory(proxy)) }
            okHttpClient?.let(::setHttpClient)
        }
    }

    /** The websocket library can tunnel only through HTTP CONNECT, so [proxy] must be an HTTP proxy. */
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
                // Credentials that were already sent are wrong: OkHttp would repeat them 20 times otherwise
                if (response.request.header(PROXY_AUTHORIZATION) != null) return@proxyAuthenticator null
                response.request.newBuilder()
                    .header(PROXY_AUTHORIZATION, credential)
                    .build()
            }
        }
        return builder.build()
    }

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
