package ru.astrainteractive.messagebridge.link.database.model

import java.util.UUID

data class LinkedPlayerModel(
    val uuid: UUID,
    val lastMinecraftName: String,
    val discordLink: DiscordLink? = null,
    val telegramLink: TelegramLink? = null
) {
    data class DiscordLink(
        val lastDiscordName: String,
        val discordId: Long,
    )

    data class TelegramLink(
        val telegramUsername: String,
        val telegramId: Long,
    )
}
