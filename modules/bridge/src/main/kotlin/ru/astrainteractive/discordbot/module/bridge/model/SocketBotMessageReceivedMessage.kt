package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedData

@Serializable
internal data class SocketBotMessageReceivedMessage(
    override val id: Long,
    val data: BotMessageReceivedData
) : SocketMessage(SocketRoute.BOT_MESSAGE_RECEIVED)
