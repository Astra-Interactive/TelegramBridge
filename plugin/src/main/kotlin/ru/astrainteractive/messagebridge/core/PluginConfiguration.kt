package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PluginConfiguration(
    @SerialName("token")
    val token: String = "",
    @SerialName("chat_id")
    val chatID: String = "",
    @SerialName("topic_id")
    val topicID: String = "",
    val displayJoinMessage: Boolean = true,
    val displayLeaveMessage: Boolean = true,
    val displayDeathMessage: Boolean = true
)
