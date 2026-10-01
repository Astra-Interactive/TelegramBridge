package ru.astrainteractive.messagebridge.link

import kotlinx.coroutines.yield
import ru.astrainteractive.messagebridge.link.player.DiscordLinkedPlayerDao
import ru.astrainteractive.messagebridge.link.player.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.player.LinkingDao
import java.util.UUID

/**
 * Keeps the players in memory. Every call suspends once, the way a call to the database does, so link changes
 * made at the same time interleave in a test.
 */
internal class FakeLinkingDao(
    vararg linkedPlayers: LinkedPlayerModel
) : LinkingDao,
    DiscordLinkedPlayerDao {
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

    override suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>> = access {
        players.values.filter { player -> player.discordLink != null }
    }
}
