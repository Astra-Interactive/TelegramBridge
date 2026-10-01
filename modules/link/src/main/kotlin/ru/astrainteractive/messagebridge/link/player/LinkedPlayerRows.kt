package ru.astrainteractive.messagebridge.link.player

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import java.util.UUID

private fun ResultRow.toDiscordLink(): LinkedPlayerModel.DiscordLink? {
    val discordId = this[LinkedPlayerTable.discordId] ?: return null
    val lastDiscordName = this[LinkedPlayerTable.lastDiscordName] ?: return null
    return LinkedPlayerModel.DiscordLink(lastDiscordName = lastDiscordName, discordId = discordId)
}

private fun ResultRow.toTelegramLink(): LinkedPlayerModel.TelegramLink? {
    val telegramId = this[LinkedPlayerTable.telegramId] ?: return null
    val telegramUsername = this[LinkedPlayerTable.lastTelegramName] ?: return null
    return LinkedPlayerModel.TelegramLink(telegramUsername = telegramUsername, telegramId = telegramId)
}

/** A link with only one of its two columns set is broken, so it reads as no link. */
internal fun ResultRow.toLinkedPlayerModel(): LinkedPlayerModel {
    return LinkedPlayerModel(
        uuid = UUID.fromString(this[LinkedPlayerTable.id].value),
        lastMinecraftName = this[LinkedPlayerTable.lastMinecraftName],
        discordLink = toDiscordLink(),
        telegramLink = toTelegramLink()
    )
}

internal fun UpdateBuilder<*>.writeLinks(linkedPlayerModel: LinkedPlayerModel) {
    this[LinkedPlayerTable.lastMinecraftName] = linkedPlayerModel.lastMinecraftName
    this[LinkedPlayerTable.discordId] = linkedPlayerModel.discordLink?.discordId
    this[LinkedPlayerTable.lastDiscordName] = linkedPlayerModel.discordLink?.lastDiscordName
    this[LinkedPlayerTable.telegramId] = linkedPlayerModel.telegramLink?.telegramId
    this[LinkedPlayerTable.lastTelegramName] = linkedPlayerModel.telegramLink?.telegramUsername
}
