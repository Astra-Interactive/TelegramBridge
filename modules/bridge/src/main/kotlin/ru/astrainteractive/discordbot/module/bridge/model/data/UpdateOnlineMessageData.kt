package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable

@Serializable
data class UpdateOnlineMessageData(
    val current: Int,
    val max: Int
) : MessageData
