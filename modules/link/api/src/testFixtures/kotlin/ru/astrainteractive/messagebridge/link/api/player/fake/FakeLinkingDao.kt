package ru.astrainteractive.messagebridge.link.api.player.fake

import kotlinx.coroutines.yield
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import java.util.UUID

class FakeLinkingDao(
    vararg linkedPlayers: LinkedPlayerModel
) : LinkingDao {
    val players = linkedPlayers.associateBy { player -> player.uuid }.toMutableMap()

    var failure: Throwable? = null

    private suspend fun <T> access(action: () -> T): Result<T> {
        yield()
        failure?.let { error -> return Result.failure(error) }
        return Result.success(action())
    }

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?> = access { players[uuid] }

    override suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel> = access {
        players[linkedPlayerModel.uuid] = linkedPlayerModel
        linkedPlayerModel
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<Unit> = access {
        players.remove(uuid)
        Unit
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel?> = access {
        players.values.firstOrNull { player -> player.discordLink?.discordId == id }
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel?> = access {
        players.values.firstOrNull { player -> player.telegramLink?.telegramId == id }
    }
}
