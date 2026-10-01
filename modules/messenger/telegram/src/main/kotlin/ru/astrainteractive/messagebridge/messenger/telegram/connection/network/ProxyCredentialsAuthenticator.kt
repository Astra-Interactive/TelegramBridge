package ru.astrainteractive.messagebridge.messenger.telegram.connection.network

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration

internal class ProxyCredentialsAuthenticator(
    private val credentials: PluginConfiguration.Proxy.Credentials,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header(PROXY_AUTHORIZATION) != null) return null
        return response.request.newBuilder()
            .header(PROXY_AUTHORIZATION, Credentials.basic(credentials.username, credentials.password))
            .build()
    }

    private companion object {
        const val PROXY_AUTHORIZATION = "Proxy-Authorization"
    }
}
