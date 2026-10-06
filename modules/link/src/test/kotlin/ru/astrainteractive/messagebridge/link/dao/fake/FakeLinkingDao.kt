package ru.astrainteractive.messagebridge.link.dao.fake

import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.link.dao.model.LinkOutcome
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
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

    override suspend fun link(uuid: UUID, minecraftName: String, account: MessengerAccount): Result<LinkOutcome> {
        linkFailure?.let { t -> return Result.failure(t) }
        val player = linkedPlayers[uuid]
            ?: LinkedPlayer(uuid = uuid, minecraftName = minecraftName, discord = null, telegram = null)
        val hasAccountOfThatMessenger = when (account) {
            is MessengerAccount.Discord -> player.discord != null
            is MessengerAccount.Telegram -> player.telegram != null
        }
        if (hasAccountOfThatMessenger) return Result.success(LinkOutcome.AlreadyLinked)
        val taken = linkedPlayers.values.any { linkedPlayer ->
            when (account) {
                is MessengerAccount.Discord -> linkedPlayer.discord?.id == account.id
                is MessengerAccount.Telegram -> linkedPlayer.telegram?.id == account.id
            }
        }
        if (taken) return Result.success(LinkOutcome.AccountTaken)
        linkedPlayers[uuid] = when (account) {
            is MessengerAccount.Discord -> player.copy(minecraftName = minecraftName, discord = account)
            is MessengerAccount.Telegram -> player.copy(minecraftName = minecraftName, telegram = account)
        }
        return Result.success(LinkOutcome.Linked)
    }

    override suspend fun unlinkDiscord(discordId: Long): Result<LinkedPlayer?> {
        deleteFailure?.let { t -> return Result.failure(t) }
        val player = linkedPlayers.values.firstOrNull { linkedPlayer -> linkedPlayer.discord?.id == discordId }
            ?: return Result.success(null)
        if (player.telegram == null) {
            linkedPlayers.remove(player.uuid)
        } else {
            linkedPlayers[player.uuid] = player.copy(discord = null)
        }
        return Result.success(player)
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<LinkedPlayer?> {
        deleteFailure?.let { t -> return Result.failure(t) }
        return Result.success(linkedPlayers.remove(uuid))
    }
}
