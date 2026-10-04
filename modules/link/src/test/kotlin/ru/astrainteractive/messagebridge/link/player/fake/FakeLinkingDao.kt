package ru.astrainteractive.messagebridge.link.player.fake

import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
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

    private fun findFirst(predicate: (LinkedPlayerModel) -> Boolean): Result<LinkedPlayerModel> {
        findFailure?.let { t -> return Result.failure(t) }
        val linkedPlayer = linkedPlayers.values.firstOrNull(predicate)
            ?: return Result.failure(NoSuchElementException("No linked player matches"))
        return Result.success(linkedPlayer)
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel> {
        return findFirst { linkedPlayer -> linkedPlayer.discordLink?.discordId == id }
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel> {
        return findFirst { linkedPlayer -> linkedPlayer.telegramLink?.telegramId == id }
    }
}
