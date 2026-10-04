package ru.astrainteractive.messagebridge.link.player.fake

import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount
import java.util.UUID

internal class FakeLinkingDao : LinkingDao {
    val linkedPlayers = mutableMapOf<UUID, LinkedPlayer>()
    var findFailure: Throwable? = null
    var linkFailure: Throwable? = null
    var deleteFailure: Throwable? = null

    private fun find(predicate: (LinkedPlayer) -> Boolean): Result<LinkedPlayer?> {
        findFailure?.let { t -> return Result.failure(t) }
        return Result.success(linkedPlayers.values.firstOrNull(predicate))
    }

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayer?> {
        return find { player -> player.uuid == uuid }
    }

    override suspend fun findByDiscordId(discordId: Long): Result<LinkedPlayer?> {
        return find { player -> player.discord?.id == discordId }
    }

    override suspend fun findByTelegramId(telegramId: Long): Result<LinkedPlayer?> {
        return find { player -> player.telegram?.id == telegramId }
    }

    override suspend fun link(uuid: UUID, minecraftName: String, account: MessengerAccount): Result<Unit> {
        linkFailure?.let { t -> return Result.failure(t) }
        val player = linkedPlayers[uuid]
            ?: LinkedPlayer(uuid = uuid, minecraftName = minecraftName, discord = null, telegram = null)
        linkedPlayers[uuid] = when (account) {
            is MessengerAccount.Discord -> player.copy(minecraftName = minecraftName, discord = account)
            is MessengerAccount.Telegram -> player.copy(minecraftName = minecraftName, telegram = account)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<LinkedPlayer?> {
        deleteFailure?.let { t -> return Result.failure(t) }
        return Result.success(linkedPlayers.remove(uuid))
    }
}
