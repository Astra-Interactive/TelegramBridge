package ru.astrainteractive.messagebridge.link.player

import java.util.UUID

interface LinkingDao {
    suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?>

    suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel>

    suspend fun deleteByUuid(uuid: UUID): Result<Unit>

    suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel?>

    suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel?>
}
