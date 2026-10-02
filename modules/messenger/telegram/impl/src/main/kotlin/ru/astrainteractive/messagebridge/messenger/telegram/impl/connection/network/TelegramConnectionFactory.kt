package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.TelegramUrl
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.ProxyType
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.model.TelegramConnectionSettings
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class TelegramConnectionFactory(
    private val dns: Dns,
) {
    private fun telegramUrlOf(apiUrl: String): TelegramUrl? {
        if (apiUrl.isBlank()) return TelegramUrl.DEFAULT_URL
        val withScheme = if (SCHEME_DELIMITER in apiUrl) apiUrl else "https://$apiUrl"
        val url = withScheme.toHttpUrlOrNull() ?: return null
        if (url.encodedPath != "/" || url.query != null) return null
        return TelegramUrl(url.scheme, url.host, url.port, false)
    }

    private fun proxyFailureOrNull(proxy: PluginConfiguration.Proxy?): TelegramFailure? = when {
        proxy == null -> null
        proxy.host.isBlank() || proxy.port !in PROXY_PORTS -> TelegramFailure.InvalidProxy(proxy)
        proxy.type == ProxyType.SOCKS5 && proxy.credentials != null -> TelegramFailure.SocksWithPassword
        else -> null
    }

    private fun createOkHttpClient(proxy: PluginConfiguration.Proxy?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .writeTimeout(WRITE_TIMEOUT.toJavaDuration())
            .readTimeout(READ_TIMEOUT.toJavaDuration())
            .pingInterval(PING_INTERVAL.toJavaDuration())
            .retryOnConnectionFailure(true)
            .dns(dns)
        if (proxy == null) return builder.build()
        val address = InetSocketAddress.createUnresolved(proxy.host, proxy.port)
        when (proxy.type) {
            ProxyType.SOCKS5 -> builder.proxy(Proxy(Proxy.Type.SOCKS, address))
            ProxyType.HTTP -> builder.proxy(Proxy(Proxy.Type.HTTP, address))
        }
        val credentials = proxy.credentials ?: return builder.build()
        return builder
            .proxyAuthenticator(ProxyCredentialsAuthenticator(credentials))
            .build()
    }

    fun create(settings: TelegramConnectionSettings): TelegramConnection {
        if (settings.token.isBlank()) return TelegramConnection.Disabled
        proxyFailureOrNull(settings.proxy)?.let { failure -> return TelegramConnection.Invalid(failure) }
        val url = telegramUrlOf(settings.apiUrl) ?: return TelegramConnection.Invalid(TelegramFailure.InvalidApiUrl)
        val okHttpClient = createOkHttpClient(settings.proxy)
        return TelegramConnection.Ready(
            token = settings.token,
            url = url,
            okHttpClient = okHttpClient,
            telegramClient = OkHttpTelegramClient(okHttpClient, settings.token, url)
        )
    }

    private companion object {
        const val SCHEME_DELIMITER = "://"
        val PROXY_PORTS = 1..65_535

        val CONNECT_TIMEOUT = 10.seconds

        val WRITE_TIMEOUT = 70.seconds

        val READ_TIMEOUT = 100.seconds

        val PING_INTERVAL = 15.seconds
    }
}
