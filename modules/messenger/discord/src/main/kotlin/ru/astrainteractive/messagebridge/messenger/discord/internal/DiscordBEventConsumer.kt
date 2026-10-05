package ru.astrainteractive.messagebridge.messenger.discord.internal

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.util.await
import kotlin.time.Duration.Companion.seconds

internal class DiscordBEventConsumer(
    private val discordChannel: Flow<DiscordChannel>,
    private val topicUpdater: DiscordTopicUpdater,
    private val embedMapper: DiscordEmbedMapper,
    private val memberResolver: DiscordMemberResolver,
    private val webhookMessageMapper: DiscordWebhookMessageMapper,
    private val bEventReceiver: BEventReceiver,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-DiscordBEventConsumer") {

    private suspend fun readyChannel(): DiscordChannel.Ready? {
        return withTimeoutOrNull(CONNECTING_TIMEOUT) {
            discordChannel.first { state -> state != DiscordChannel.Connecting }
        }?.tryCast<DiscordChannel.Ready>()
    }

    private suspend fun sendText(event: Text, channel: TextChannel, webhookClient: WebhookClient) {
        val member = memberResolver.resolve(channel, event)
        val message = webhookMessageMapper.map(event, member)
        webhookClient.send(message).await()
    }

    private suspend fun sendServerStatus(channel: TextChannel, text: String) {
        topicUpdater.setStarting(channel)
        channel.sendMessage(text).await()
    }

    private suspend fun send(bEvent: BEvent, ready: DiscordChannel.Ready) {
        val channel = ready.textChannel
        when (bEvent) {
            is PlayerDeathBEvent -> channel.sendMessageEmbeds(embedMapper.map(bEvent)).await()
            is PlayerJoinedBEvent -> {
                topicUpdater.updateOnlineCount(channel)
                channel.sendMessageEmbeds(embedMapper.map(bEvent)).await()
            }

            is PlayerLeaveBEvent -> {
                topicUpdater.updateOnlineCount(channel)
                channel.sendMessageEmbeds(embedMapper.map(bEvent)).await()
            }

            is Text -> sendText(bEvent, channel, ready.webhookClient)
            ServerClosedBEvent -> channel.sendMessage(SERVER_CLOSED_MESSAGE).await()
            ServerOpenBEvent -> sendServerStatus(channel, SERVER_OPEN_MESSAGE)
        }
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.DISCORD) return
        val ready = readyChannel() ?: run {
            verbose { "#consume Discord is not ready, skipped $bEvent" }
            return
        }
        runCatching { send(bEvent, ready) }
            .propagateCancellationException()
            .onFailure { t -> error(t) { "#consume could not send $bEvent" } }
    }

    init {
        bEventReceiver
            .bEvents(this)
            .onEach { bEvent -> verbose { "#init receive event $bEvent" } }
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(this)
    }

    private companion object {
        const val SERVER_CLOSED_MESSAGE = "🛑 **Сервер остановлен**"
        const val SERVER_OPEN_MESSAGE = "✅ **Сервер успешно запущен**"
        val CONNECTING_TIMEOUT = 30.seconds
    }
}
