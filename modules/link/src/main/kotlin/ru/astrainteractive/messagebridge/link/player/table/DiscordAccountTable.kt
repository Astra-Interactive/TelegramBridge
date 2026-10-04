package ru.astrainteractive.messagebridge.link.player.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object DiscordAccountTable : Table("link_discord_account") {
    val playerUuid = reference("player_uuid", PlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
    val discordId = long("discord_id").uniqueIndex()
    val discordName = text("discord_name")

    override val primaryKey = PrimaryKey(playerUuid)
}
