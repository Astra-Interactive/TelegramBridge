package ru.astrainteractive.messagebridge.messenger.discord.impl.channel.network

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.internal.DiscordDeliveryError
import kotlin.time.Duration.Companion.seconds

internal class DiscordWebhookClients(
    private val factory: WebHookClientFactory,
    private val deliveryError: DiscordDeliveryError,
) {
    private suspend fun createWithRetry(
        jda: JDA,
        channelId: String,
        onFailure: suspend (Throwable) -> Unit
    ): WebhookClient {
        while (true) {
            factory.create(jda, channelId).fold(
                onSuccess = { client -> return client },
                onFailure = { t ->
                    deliveryError.report(t)
                    onFailure.invoke(t)
                    delay(RETRY_DELAY)
                }
            )
        }
    }

    private fun clientsOf(jda: JDA?, channelId: String): Flow<Result<WebhookClient>?> {
        if (jda == null || channelId.isBlank()) return flowOf(null)
        return channelFlow {
            send(null)
            val client = createWithRetry(jda, channelId) { t -> send(Result.failure(t)) }
            deliveryError.clear()
            send(Result.success(client))
            try {
                awaitCancellation()
            } finally {
                client.close()
            }
        }
    }

    fun clients(jdaFlow: Flow<JDA?>, channelIdFlow: Flow<String>): Flow<Result<WebhookClient>?> {
        return combine(jdaFlow, channelIdFlow, ::clientsOf).flatMapLatest { clients -> clients }
    }

    private companion object {
        val RETRY_DELAY = 30.seconds
    }
}
