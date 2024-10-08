package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.Serializable

@Serializable
class PluginConfiguration(
    val token: String = "",
    val channelID: String = "",
    val topicID: String = "",
    val displayJoinMessage: Boolean = true,
    val displayLeaveMessage: Boolean = true,
    val displayDeathMessage: Boolean = true
)
