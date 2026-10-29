package ru.astrainteractive.messagebridge.messenger.discord.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import kotlin.time.Duration.Companion.seconds

internal class DiscordChannelConnector(
    private val webhookConnector: DiscordWebhookConnector
) : Logger by JUtiltLogger("MessageBridge-DiscordChannelConnector") {
    fun connect(jda: JDA, channelId: String): Flow<DiscordChannel> {
        return callbackFlow<DiscordChannel> {
            val textChannel = jda.getTextChannelById(channelId) ?: error("Could not find channel $channelId")
            val webhookClient = webhookConnector.connect(jda, channelId).first()
            send(DiscordChannel.Ready(textChannel = textChannel, webhookClient = webhookClient))
            awaitClose {
                webhookClient.close()
            }
        }.retryWhen { t, _ ->
            error { "#connect could not open channel $channelId: ${t.message}" }
            emit(DiscordChannel.Failed)
            delay(RETRY_DELAY)
            t !is CancellationException
        }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
