@file:Suppress("MagicNumber")

package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

private fun buildJdaOkHttpClientBuilder(): OkHttpClient.Builder {
    return OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 25 })
        .connectionPool(
            ConnectionPool(
                maxIdleConnections = 5,
                keepAliveDuration = 10,
                timeUnit = TimeUnit.SECONDS
            )
        )
}

private fun PluginConfiguration.Proxy.buildOkHttpClient(): OkHttpClient {
    return buildJdaOkHttpClientBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
        .proxyAuthenticator { route, response ->
            val builder = response.request.newBuilder()
            if (route?.socketAddress?.hostString == host) {
                val credential: String = Credentials.basic(username, password)
                builder.header("Proxy-Authorization", credential)
            }
            builder.build()
        }
        .build()
}

internal fun PluginConfiguration.Proxy?.openOkHttpClient(): Flow<OkHttpClient> {
    return callbackFlow {
        val okHttpClient = this@openOkHttpClient?.buildOkHttpClient() ?: buildJdaOkHttpClientBuilder().build()
        send(okHttpClient)

        awaitClose {
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            okHttpClient.cache?.close()
        }
    }
}
