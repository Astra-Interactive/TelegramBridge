package ru.astrainteractive.messagebridge.link.impl.player.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object TelegramLinkTable : Table("telegram_link") {
    val uuid = reference("uuid", MinecraftPlayerTable, onDelete = ReferenceOption.CASCADE)
    val telegramId = long("telegram_id").uniqueIndex()
    val lastTelegramName = text("last_telegram_name")

    override val primaryKey = PrimaryKey(uuid)
}
