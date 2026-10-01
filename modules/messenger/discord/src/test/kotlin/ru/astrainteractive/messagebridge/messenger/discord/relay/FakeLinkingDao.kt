package ru.astrainteractive.messagebridge.messenger.discord.relay

import ru.astrainteractive.messagebridge.link.player.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.player.LinkingDao
import java.util.UUID

internal class FakeLinkingDao(
    vararg linkedPlayers: LinkedPlayerModel
) : LinkingDao {
    private val playerByUuid = linkedPlayers.associateBy { player -> player.uuid }.toMutableMap()

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?> = Result.success(playerByUuid[uuid])

    override suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel> {
        playerByUuid[linkedPlayerModel.uuid] = linkedPlayerModel
        return Result.success(linkedPlayerModel)
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<Unit> {
        playerByUuid.remove(uuid)
        return Result.success(Unit)
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel?> {
        return Result.success(playerByUuid.values.firstOrNull { player -> player.discordLink?.discordId == id })
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel?> {
        return Result.success(playerByUuid.values.firstOrNull { player -> player.telegramLink?.telegramId == id })
    }
}
