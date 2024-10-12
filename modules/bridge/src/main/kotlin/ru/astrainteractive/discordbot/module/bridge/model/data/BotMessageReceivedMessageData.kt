package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable

@Serializable
class BotMessageReceivedMessageData(
    val message: String,
    val fromUserId: Long
) : MessageData
