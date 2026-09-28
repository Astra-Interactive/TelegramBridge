package ru.astrainteractive.messagebridge.commands.fake

import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import java.util.UUID

internal class FakeLinkingDao : LinkingDao {
    val linkedPlayers = mutableMapOf<UUID, LinkedPlayerModel>()
    var findFailure: Throwable? = null
    var deleteFailure: Throwable? = null

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?> {
        val failure = findFailure ?: return Result.success(linkedPlayers[uuid])
        return Result.failure(failure)
    }

    override suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel> {
        linkedPlayers[linkedPlayerModel.uuid] = linkedPlayerModel
        return Result.success(linkedPlayerModel)
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<Unit> {
        val failure = deleteFailure
        if (failure != null) {
            return Result.failure(failure)
        }
        linkedPlayers.remove(uuid)
        return Result.success(Unit)
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel> {
        error("Minecraft commands never look a player up by a Discord id")
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel> {
        error("Minecraft commands never look a player up by a Telegram id")
    }
}
