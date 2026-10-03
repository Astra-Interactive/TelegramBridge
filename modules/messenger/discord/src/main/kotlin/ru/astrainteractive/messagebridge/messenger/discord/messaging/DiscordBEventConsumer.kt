package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.api.tryConsume
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class DiscordBEventConsumer(
    private val channelProvider: DiscordChannelProvider,
    private val topicUpdater: DiscordTopicUpdater,
    private val embedMapper: DiscordEmbedMapper,
    private val memberResolver: DiscordMemberResolver,
    private val webhookMessageMapper: DiscordWebhookMessageMapper,
    private val bEventReceiver: BEventReceiver,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-DiscordBEventConsumer").withoutParentHandlers() {

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.DISCORD) return
        val channel = channelProvider.textChannel() ?: run {
            error { "#consume could not get text channel" }
            return
        }
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

            is Text -> sendText(bEvent, channel)
            ServerClosedBEvent -> channel.sendMessage(SERVER_CLOSED_MESSAGE).await()
            ServerOpenBEvent -> sendServerStatus(channel, SERVER_OPEN_MESSAGE)
        }
    }

    private suspend fun sendText(event: Text, channel: TextChannel) {
        val member = memberResolver.resolve(channel, event)
        val message = webhookMessageMapper.map(event, member)
        channelProvider.webHookClient().send(message)
    }

    private suspend fun sendServerStatus(channel: TextChannel, text: String) {
        topicUpdater.setStarting(channel)
        channel.sendMessage(text).await()
    }

    init {
        bEventReceiver
            .bEvents(this)
            .onEach { bEvent -> verbose { "#init receive event $bEvent" } }
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(this)
    }

    private companion object {
        const val SERVER_CLOSED_MESSAGE = "🛑 **Сервер остановлен**"
        const val SERVER_OPEN_MESSAGE = "✅ **Сервер успешно запущен**"
    }
}
