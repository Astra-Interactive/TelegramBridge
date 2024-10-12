package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedData

@Serializable
internal data class SocketBotMessageReceivedMessage(
    override val id: Long,
    val data: BotMessageReceivedData
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.BOT_MESSAGE_RECEIVED

    @Transient
    override val created: Instant = Clock.System.now()
}
