package ru.astrainteractive.messagebridge.messenger.discord.channel

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordDeliveryError
import kotlin.time.Duration.Companion.seconds

/**
 * A webhook client for the bridge channel of the connected bot. It is made again when the bot reconnects or the
 * channel changes, and a failed attempt is repeated until the webhook can be made.
 */
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
                onFailure = { failure ->
                    deliveryError.report(failure)
                    onFailure(failure)
                    delay(RETRY_DELAY)
                }
            )
        }
    }

    /** Emits `null` while the client is made, then the client or why it could not be made yet. */
    private fun clientsOf(jda: JDA?, channelId: String): Flow<Result<WebhookClient>?> {
        if (jda == null || channelId.isBlank()) return flowOf(null)
        return channelFlow {
            send(null)
            val client = createWithRetry(jda, channelId) { failure -> send(Result.failure(failure)) }
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
