@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import okhttp3.Credentials
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProxyCredentialsAuthenticatorTest {
    private val authenticator = ProxyCredentialsAuthenticator(
        credentials = PluginConfiguration.Proxy.Credentials(username = "user", password = "secret")
    )

    private fun proxyAuthRequiredFor(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(PROXY_AUTH_REQUIRED)
        .message("Proxy Authentication Required")
        .build()

    @Test
    fun GIVEN_proxy_asks_for_credentials_WHEN_authenticated_THEN_request_carries_them() {
        val request = Request.Builder().url("https://api.telegram.org/").build()

        val retried = authenticator.authenticate(route = null, response = proxyAuthRequiredFor(request))

        assertEquals(Credentials.basic("user", "secret"), retried?.header("Proxy-Authorization"))
    }

    @Test
    fun GIVEN_proxy_rejected_the_credentials_WHEN_authenticated_THEN_they_are_not_sent_again() {
        val request = Request.Builder()
            .url("https://api.telegram.org/")
            .header("Proxy-Authorization", Credentials.basic("user", "secret"))
            .build()

        assertNull(authenticator.authenticate(route = null, response = proxyAuthRequiredFor(request)))
    }

    private companion object {
        const val PROXY_AUTH_REQUIRED = 407
    }
}
