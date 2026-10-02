package ru.astrainteractive.messagebridge.link.impl.player.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object DiscordLinkTable : Table("discord_link") {
    val uuid = reference("uuid", MinecraftPlayerTable, onDelete = ReferenceOption.CASCADE)
    val discordId = long("discord_id").uniqueIndex()
    val lastDiscordName = text("last_discord_name")

    override val primaryKey = PrimaryKey(uuid)
}
