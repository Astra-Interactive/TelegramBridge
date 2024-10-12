package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable

@Serializable
data class UpdateOnlineData(
    val current: Int,
    val max: Int
)
