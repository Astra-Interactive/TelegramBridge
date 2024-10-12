package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedMessageData

@Serializable
internal data class SocketBotMessageReceivedMessage(
    override val id: Long,
    override val data: BotMessageReceivedMessageData
) : SocketMessage(SocketRoute.BOT_MESSAGE_RECEIVED)
