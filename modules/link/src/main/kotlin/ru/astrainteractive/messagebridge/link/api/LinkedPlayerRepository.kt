package ru.astrainteractive.messagebridge.link.api

import ru.astrainteractive.messagebridge.link.model.LinkOutcome
import ru.astrainteractive.messagebridge.link.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.model.MessengerAccount
import java.util.UUID

internal interface LinkedPlayerRepository {
    suspend fun findByUuid(uuid: UUID): Result<LinkedPlayer?>
    suspend fun findByDiscordId(discordId: Long): Result<LinkedPlayer?>
    suspend fun findByTelegramId(telegramId: Long): Result<LinkedPlayer?>
    suspend fun link(uuid: UUID, minecraftName: String, account: MessengerAccount): Result<LinkOutcome>
    suspend fun deleteByUuid(uuid: UUID): Result<LinkedPlayer?>
    suspend fun unlinkDiscord(discordId: Long): Result<LinkedPlayer?>
}
