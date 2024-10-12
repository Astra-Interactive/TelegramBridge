package ru.astrainteractive.messagebridge.discord.core

import kotlinx.serialization.Serializable

@Serializable
data class JdaConfig(
    val token: String = "",
    val activity: String = ""
)
