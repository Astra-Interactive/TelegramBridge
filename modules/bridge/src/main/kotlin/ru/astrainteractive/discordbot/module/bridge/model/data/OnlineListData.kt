package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable

@Serializable
data class OnlineListData(
    val onlinePlayers: List<String>
)
