package ru.astrainteractive.messagebridge.messenger.telegram.connection

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.astrainteractive.messagebridge.core.PluginConfiguration

/** Answers `407 Proxy Authentication Required` with the credentials of the proxy, once per request. */
internal class ProxyCredentialsAuthenticator(
    private val credentials: PluginConfiguration.Proxy.Credentials,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Credentials the proxy already rejected would be sent again and again
        if (response.request.header(PROXY_AUTHORIZATION) != null) return null
        return response.request.newBuilder()
            .header(PROXY_AUTHORIZATION, Credentials.basic(credentials.username, credentials.password))
            .build()
    }

    private companion object {
        const val PROXY_AUTHORIZATION = "Proxy-Authorization"
    }
}
