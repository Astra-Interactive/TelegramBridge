package ru.astrainteractive.messagebridge.link.database.dao

import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import java.util.UUID

interface LinkingDao {
    suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?>
    suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel>
    suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel>
    suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel>
}
