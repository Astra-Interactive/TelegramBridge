package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable

@Serializable
data class OnlineListMessageData(
    val onlinePlayers: List<String>
) : MessageData
