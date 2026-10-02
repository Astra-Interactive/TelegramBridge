package ru.astrainteractive.messagebridge.link.impl.player.database

import org.jetbrains.exposed.v1.core.ResultRow
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import java.util.UUID

private fun ResultRow.toDiscordLink(): LinkedPlayerModel.DiscordLink? {
    val discordId = getOrNull(DiscordLinkTable.discordId) ?: return null
    return LinkedPlayerModel.DiscordLink(
        lastDiscordName = this[DiscordLinkTable.lastDiscordName],
        discordId = discordId
    )
}

private fun ResultRow.toTelegramLink(): LinkedPlayerModel.TelegramLink? {
    val telegramId = getOrNull(TelegramLinkTable.telegramId) ?: return null
    return LinkedPlayerModel.TelegramLink(
        telegramUsername = this[TelegramLinkTable.lastTelegramName],
        telegramId = telegramId
    )
}

internal fun ResultRow.toLinkedPlayerModel(): LinkedPlayerModel {
    return LinkedPlayerModel(
        uuid = UUID.fromString(this[MinecraftPlayerTable.id].value),
        lastMinecraftName = this[MinecraftPlayerTable.lastMinecraftName],
        discordLink = toDiscordLink(),
        telegramLink = toTelegramLink()
    )
}
