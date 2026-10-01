package ru.astrainteractive.messagebridge.link.player

import java.util.UUID

data class LinkedPlayerModel(
    val uuid: UUID,
    val lastMinecraftName: String,
    val discordLink: DiscordLink?,
    val telegramLink: TelegramLink?
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
