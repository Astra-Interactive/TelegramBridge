package ru.astrainteractive.messagebridge.link.player.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object TelegramAccountTable : Table("link_telegram_account") {
    val playerUuid = reference("player_uuid", PlayerTable.uuid, onDelete = ReferenceOption.CASCADE)
    val telegramId = long("telegram_id").uniqueIndex()
    val telegramUsername = text("telegram_username")

    override val primaryKey = PrimaryKey(playerUuid)
}
