package ru.astrainteractive.messagebridge.link.dao.api

import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import java.util.UUID

internal interface LinkingDao {
    suspend fun findByUuid(uuid: UUID): Result<LinkedPlayer?>
    suspend fun findByDiscordId(discordId: Long): Result<LinkedPlayer?>
    suspend fun findByTelegramId(telegramId: Long): Result<LinkedPlayer?>
    suspend fun link(uuid: UUID, minecraftName: String, account: MessengerAccount): Result<Unit>
    suspend fun deleteByUuid(uuid: UUID): Result<LinkedPlayer?>
    suspend fun unlinkDiscord(discordId: Long): Result<LinkedPlayer?>
}
