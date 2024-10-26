package ru.astrainteractive.messagebridge.link.database.table

import ru.astrainteractive.astralibs.exposed.table.StringIdTable

object LinkedPlayerTable : StringIdTable("LinkedPlayerTable", "uuid") {
    val lastMinecraftName = text("last_minecraft_name")

    val lastDiscordName = text("last_discord_name").nullable()
    val discordId = long("discord_id").nullable()
    val lastTelegramName = text("last_telegram_name").nullable()
    val telegramId = long("telegram_id").nullable()
}
