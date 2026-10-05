package ru.astrainteractive.messagebridge.link.dao.internal

import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayerStorageError
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.dao.table.DiscordAccountTable
import ru.astrainteractive.messagebridge.link.dao.table.PlayerTable
import ru.astrainteractive.messagebridge.link.dao.table.TelegramAccountTable
import java.util.UUID

internal class LinkingDaoImpl(
    private val databaseState: StateFlow<Database?>
) : LinkingDao {
    private val linkedPlayers = PlayerTable
        .join(DiscordAccountTable, JoinType.LEFT, PlayerTable.uuid, DiscordAccountTable.playerUuid)
        .join(TelegramAccountTable, JoinType.LEFT, PlayerTable.uuid, TelegramAccountTable.playerUuid)

    private fun ResultRow.toLinkedPlayer(): LinkedPlayer {
        val discordId = getOrNull(DiscordAccountTable.discordId)
        val telegramId = getOrNull(TelegramAccountTable.telegramId)
        return LinkedPlayer(
            uuid = this[PlayerTable.uuid],
            minecraftName = this[PlayerTable.minecraftName],
            discord = discordId?.let { id ->
                MessengerAccount.Discord(id = id, name = this[DiscordAccountTable.discordName])
            },
            telegram = telegramId?.let { id ->
                MessengerAccount.Telegram(id = id, username = this[TelegramAccountTable.telegramUsername])
            }
        )
    }

    private fun findWhere(condition: Op<Boolean>): LinkedPlayer? {
        return linkedPlayers.selectAll()
            .where(condition)
            .limit(1)
            .map { row -> row.toLinkedPlayer() }
            .firstOrNull()
    }

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayer?> {
        val database = databaseState.value
            ?: return Result.failure(
                LinkedPlayerStorageError("Could not find player $uuid: the link database is not open", null)
            )
        return runCatching { suspendTransaction(database) { findWhere(PlayerTable.uuid eq uuid) } }
            .propagateCancellationException()
            .fold(
                onSuccess = { player -> Result.success(player) },
                onFailure = { t -> Result.failure(LinkedPlayerStorageError("Could not find player $uuid", t)) }
            )
    }

    override suspend fun findByDiscordId(discordId: Long): Result<LinkedPlayer?> {
        val database = databaseState.value
            ?: return Result.failure(
                LinkedPlayerStorageError(
                    "Could not find Discord account $discordId: the link database is not open",
                    null
                )
            )
        return runCatching { suspendTransaction(database) { findWhere(DiscordAccountTable.discordId eq discordId) } }
            .propagateCancellationException()
            .fold(
                onSuccess = { player -> Result.success(player) },
                onFailure = { t ->
                    Result.failure(LinkedPlayerStorageError("Could not find Discord account $discordId", t))
                }
            )
    }

    override suspend fun findByTelegramId(telegramId: Long): Result<LinkedPlayer?> {
        val database = databaseState.value
            ?: return Result.failure(
                LinkedPlayerStorageError(
                    "Could not find Telegram account $telegramId: the link database is not open",
                    null
                )
            )
        return runCatching { suspendTransaction(database) { findWhere(TelegramAccountTable.telegramId eq telegramId) } }
            .propagateCancellationException()
            .fold(
                onSuccess = { player -> Result.success(player) },
                onFailure = { t ->
                    Result.failure(LinkedPlayerStorageError("Could not find Telegram account $telegramId", t))
                }
            )
    }

    override suspend fun link(uuid: UUID, minecraftName: String, account: MessengerAccount): Result<Unit> {
        val database = databaseState.value
            ?: return Result.failure(
                LinkedPlayerStorageError("Could not link $account to $uuid: the link database is not open", null)
            )
        return runCatching {
            suspendTransaction(database) {
                val alreadyLinked = when (account) {
                    is MessengerAccount.Discord -> DiscordAccountTable.selectAll().where {
                        (DiscordAccountTable.discordId eq account.id) or (DiscordAccountTable.playerUuid eq uuid)
                    }

                    is MessengerAccount.Telegram -> TelegramAccountTable.selectAll().where {
                        (TelegramAccountTable.telegramId eq account.id) or (TelegramAccountTable.playerUuid eq uuid)
                    }
                }.empty().not()
                if (alreadyLinked) return@suspendTransaction false
                PlayerTable.upsert { statement ->
                    statement[PlayerTable.uuid] = uuid
                    statement[PlayerTable.minecraftName] = minecraftName
                }
                when (account) {
                    is MessengerAccount.Discord -> DiscordAccountTable.insert { statement ->
                        statement[DiscordAccountTable.playerUuid] = uuid
                        statement[DiscordAccountTable.discordId] = account.id
                        statement[DiscordAccountTable.discordName] = account.name
                    }

                    is MessengerAccount.Telegram -> TelegramAccountTable.insert { statement ->
                        statement[TelegramAccountTable.playerUuid] = uuid
                        statement[TelegramAccountTable.telegramId] = account.id
                        statement[TelegramAccountTable.telegramUsername] = account.username
                    }
                }
                true
            }
        }
            .propagateCancellationException()
            .fold(
                onSuccess = { linked ->
                    if (linked) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            LinkedPlayerStorageError("Could not link $account to $uuid: already linked", null)
                        )
                    }
                },
                onFailure = { t -> Result.failure(LinkedPlayerStorageError("Could not link $account to $uuid", t)) }
            )
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<LinkedPlayer?> {
        val database = databaseState.value
            ?: return Result.failure(
                LinkedPlayerStorageError("Could not unlink player $uuid: the link database is not open", null)
            )
        return runCatching {
            suspendTransaction(database) {
                val player = findWhere(PlayerTable.uuid eq uuid)
                PlayerTable.deleteWhere { PlayerTable.uuid eq uuid }
                player
            }
        }
            .propagateCancellationException()
            .fold(
                onSuccess = { player -> Result.success(player) },
                onFailure = { t -> Result.failure(LinkedPlayerStorageError("Could not unlink player $uuid", t)) }
            )
    }

    override suspend fun unlinkDiscord(discordId: Long): Result<LinkedPlayer?> {
        val database = databaseState.value
            ?: return Result.failure(
                LinkedPlayerStorageError(
                    "Could not unlink Discord account $discordId: the link database is not open",
                    null
                )
            )
        return runCatching {
            suspendTransaction(database) {
                val player = findWhere(DiscordAccountTable.discordId eq discordId)
                    ?: return@suspendTransaction null
                DiscordAccountTable.deleteWhere { DiscordAccountTable.discordId eq discordId }
                if (player.telegram == null) {
                    PlayerTable.deleteWhere { PlayerTable.uuid eq player.uuid }
                }
                player
            }
        }
            .propagateCancellationException()
            .fold(
                onSuccess = { player -> Result.success(player) },
                onFailure = { t ->
                    Result.failure(LinkedPlayerStorageError("Could not unlink Discord account $discordId", t))
                }
            )
    }
}
