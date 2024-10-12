package ru.astrainteractive.messagebridge.discord.event

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.RequestOnlineMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.messagebridge.discord.event.core.DiscordEventListener
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class MessageEventListener(
    private val serverBridgeApi: BridgeApi
) : ListenerAdapter(),
    DiscordEventListener,
    CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO) {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isWebhookMessage) return
        if (event.author.isBot) return
        if (event.channelType == ChannelType.PRIVATE) {
            val data = BotMessageReceivedMessageData(
                message = event.message.contentRaw,
                fromUserId = event.author.id
            )
            launch { serverBridgeApi.broadcastEvent(data) }
            return
        }
        if (event.message.channelId != "756872937696526377") return
        if (event.message.contentRaw == "!vanilla") {
            launch { serverBridgeApi.broadcastEvent(RequestOnlineMessageData) }
            return
        }
        val serverEvent = ServerEvent.Text.Discord(
            author = event.member?.nickname ?: event.author.name,
            text = event.message.contentRaw
        )
        launch(Dispatchers.IO) {
            serverBridgeApi.broadcastEvent(ServerEventMessageData(serverEvent))
        }
    }
}
