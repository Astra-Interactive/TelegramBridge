package ru.astrainteractive.messagebridge.messenger.telegram.di.factory

import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.TelegramUrl
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.ProxyType
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnectionSettings
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramFailure
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class TelegramConnectionFactory {
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val resolved = Dns.SYSTEM.lookup(hostname)
            return resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved }
        }
    }

    fun create(settings: TelegramConnectionSettings): TelegramConnection {
        if (settings.token.isBlank()) return TelegramConnection.Disabled
        val proxy = settings.proxy
        if (proxy?.type == ProxyType.SOCKS5 && proxy.credentials != null) {
            return TelegramConnection.Invalid(TelegramFailure.SocksWithPassword)
        }
        val url = telegramUrlOf(settings.apiUrl) ?: return TelegramConnection.Invalid(TelegramFailure.InvalidApiUrl)
        val okHttpClient = createOkHttpClient(proxy)
        return TelegramConnection.Ready(
            token = settings.token,
            url = url,
            okHttpClient = okHttpClient,
            telegramClient = OkHttpTelegramClient(okHttpClient, settings.token, url)
        )
    }

    /** @return `null` when [apiUrl] is not an address of a server, e.g. it has a path the Bot API URL cannot keep */
    private fun telegramUrlOf(apiUrl: String): TelegramUrl? {
        if (apiUrl.isBlank()) return TelegramUrl.DEFAULT_URL
        val withScheme = if (SCHEME_DELIMITER in apiUrl) apiUrl else "https://$apiUrl"
        val url = withScheme.toHttpUrlOrNull() ?: return null
        if (url.encodedPath != "/" || url.query != null) return null
        return TelegramUrl(url.scheme, url.host, url.port, false)
    }

    private fun createOkHttpClient(proxy: PluginConfiguration.Proxy?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .writeTimeout(WRITE_TIMEOUT.toJavaDuration())
            .readTimeout(READ_TIMEOUT.toJavaDuration())
            .pingInterval(PING_INTERVAL.toJavaDuration())
            .retryOnConnectionFailure(true)
            .dns(ipv4FirstDns)
        if (proxy == null) return builder.build()
        val address = InetSocketAddress.createUnresolved(proxy.host, proxy.port)
        when (proxy.type) {
            ProxyType.SOCKS5 -> builder.proxy(Proxy(Proxy.Type.SOCKS, address))
            ProxyType.HTTP -> builder.proxy(Proxy(Proxy.Type.HTTP, address))
        }
        val (username, password) = proxy.credentials ?: return builder.build()
        builder.proxyAuthenticator { _, response ->
            // Credentials the proxy already rejected would be sent again and again
            if (response.request.header(PROXY_AUTHORIZATION) != null) return@proxyAuthenticator null
            response.request.newBuilder()
                .header(PROXY_AUTHORIZATION, Credentials.basic(username, password))
                .build()
        }
        return builder.build()
    }

    private companion object {
        const val SCHEME_DELIMITER = "://"
        const val PROXY_AUTHORIZATION = "Proxy-Authorization"

        /** Spent on every unreachable address before the next one is tried. */
        val CONNECT_TIMEOUT = 10.seconds

        val WRITE_TIMEOUT = 70.seconds

        /** Must exceed the getUpdates timeout, which holds the connection open. */
        val READ_TIMEOUT = 100.seconds

        val PING_INTERVAL = 15.seconds
    }
}
