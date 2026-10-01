@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.connection

import okhttp3.Dns
import org.telegram.telegrambots.meta.TelegramUrl
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.ProxyType
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TelegramConnectionFactoryTest {
    private val dns = Dns.SYSTEM
    private val factory = TelegramConnectionFactory(dns = dns)
    private val readyConnections = mutableListOf<TelegramConnection.Ready>()

    private fun settingsOf(
        token: String = "123:token",
        proxy: PluginConfiguration.Proxy? = null,
        apiUrl: String = ""
    ) = TelegramConnectionSettings(token = token, proxy = proxy, apiUrl = apiUrl)

    private fun readyOf(settings: TelegramConnectionSettings): TelegramConnection.Ready {
        val connection = assertIs<TelegramConnection.Ready>(factory.create(settings))
        readyConnections += connection
        return connection
    }

    @AfterTest
    fun closeConnections() {
        readyConnections.forEach(TelegramConnection.Ready::close)
    }

    @Test
    fun GIVEN_blank_token_WHEN_created_THEN_telegram_is_disabled() {
        assertEquals(TelegramConnection.Disabled, factory.create(settingsOf(token = " ")))
    }

    @Test
    fun GIVEN_socks_proxy_with_password_WHEN_created_THEN_settings_are_invalid() {
        val proxy = PluginConfiguration.Proxy(
            type = ProxyType.SOCKS5,
            host = "127.0.0.1",
            port = 1080,
            username = "user",
            password = "secret"
        )

        val connection = factory.create(settingsOf(proxy = proxy))

        assertEquals(TelegramConnection.Invalid(TelegramFailure.SocksWithPassword), connection)
    }

    @Test
    fun GIVEN_socks_proxy_without_password_WHEN_created_THEN_requests_go_through_it() {
        val proxy = PluginConfiguration.Proxy(type = ProxyType.SOCKS5, host = "127.0.0.1", port = 1080)

        val connection = readyOf(settingsOf(proxy = proxy))

        assertEquals(
            Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 1080)),
            connection.okHttpClient.proxy
        )
    }

    @Test
    fun GIVEN_http_proxy_with_password_WHEN_created_THEN_proxy_answers_with_credentials() {
        val proxy = PluginConfiguration.Proxy(
            type = ProxyType.HTTP,
            host = "127.0.0.1",
            port = 3128,
            username = "user",
            password = "secret"
        )

        val connection = readyOf(settingsOf(proxy = proxy))

        assertEquals(Proxy.Type.HTTP, connection.okHttpClient.proxy?.type())
        assertIs<ProxyCredentialsAuthenticator>(connection.okHttpClient.proxyAuthenticator)
    }

    @Test
    fun GIVEN_proxy_with_port_out_of_range_WHEN_created_THEN_proxy_is_invalid() {
        listOf(0, -1, 65_536).forEach { port ->
            val proxy = PluginConfiguration.Proxy(host = "127.0.0.1", port = port)

            val connection = factory.create(settingsOf(proxy = proxy))

            assertEquals(TelegramConnection.Invalid(TelegramFailure.InvalidProxy(proxy)), connection, "$port")
        }
    }

    @Test
    fun GIVEN_proxy_without_host_WHEN_created_THEN_proxy_is_invalid() {
        val proxy = PluginConfiguration.Proxy(host = " ", port = 3128)

        val connection = factory.create(settingsOf(proxy = proxy))

        assertEquals(TelegramConnection.Invalid(TelegramFailure.InvalidProxy(proxy)), connection)
    }

    @Test
    fun GIVEN_no_proxy_WHEN_created_THEN_requests_go_directly() {
        val connection = readyOf(settingsOf())

        assertNull(connection.okHttpClient.proxy)
        assertSame(dns, connection.okHttpClient.dns)
    }

    @Test
    fun GIVEN_blank_api_url_WHEN_created_THEN_telegram_server_is_used() {
        assertEquals(TelegramUrl.DEFAULT_URL, readyOf(settingsOf(apiUrl = "")).url)
    }

    @Test
    fun GIVEN_api_url_without_scheme_WHEN_created_THEN_https_is_used() {
        val url = readyOf(settingsOf(apiUrl = "tg.example.com:8081")).url

        assertEquals(TelegramUrl("https", "tg.example.com", 8081, false), url)
    }

    @Test
    fun GIVEN_api_url_with_scheme_WHEN_created_THEN_scheme_is_kept() {
        val url = readyOf(settingsOf(apiUrl = "http://127.0.0.1:8081/")).url

        assertEquals(TelegramUrl("http", "127.0.0.1", 8081, false), url)
    }

    @Test
    fun GIVEN_api_url_with_path_or_query_WHEN_created_THEN_api_url_is_invalid() {
        listOf("https://tg.example.com/bot", "https://tg.example.com/?a=b", "https://").forEach { apiUrl ->
            val connection = factory.create(settingsOf(apiUrl = apiUrl))

            assertEquals(TelegramConnection.Invalid(TelegramFailure.InvalidApiUrl), connection, apiUrl)
        }
    }

    @Test
    fun GIVEN_ready_connection_WHEN_closed_THEN_its_threads_stop() {
        val connection = readyOf(settingsOf())

        connection.close()

        assertTrue(connection.okHttpClient.dispatcher.executorService.isShutdown)
    }
}
