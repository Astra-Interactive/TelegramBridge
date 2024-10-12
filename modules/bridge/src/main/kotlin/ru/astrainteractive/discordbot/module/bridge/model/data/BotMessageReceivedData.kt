package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable

@Serializable
class BotMessageReceivedData(
    val message: String,
    val fromUserId: Long
)
