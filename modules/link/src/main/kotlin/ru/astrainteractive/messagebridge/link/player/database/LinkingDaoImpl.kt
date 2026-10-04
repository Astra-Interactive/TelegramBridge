package ru.astrainteractive.messagebridge.link.player.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerStorageError
import ru.astrainteractive.messagebridge.link.player.model.MessengerAccount
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal class LinkingDaoImpl(
    private val databaseFlow: Flow<Database>
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

    private suspend fun <T> query(action: String, statement: JdbcTransaction.() -> T): Result<T> {
        val database = withTimeoutOrNull(DATABASE_TIMEOUT) { databaseFlow.first() }
            ?: return Result.failure(LinkedPlayerStorageError("Could not $action: the link database is not open", null))
        return runCatching { suspendTransaction(database) { statement.invoke(this) } }
            .propagateCancellationException()
            .fold(
                onSuccess = { value -> Result.success(value) },
                onFailure = { t -> Result.failure(LinkedPlayerStorageError("Could not $action", t)) }
            )
    }

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayer?> = query("find player $uuid") {
        findWhere(PlayerTable.uuid eq uuid)
    }

    override suspend fun findByDiscordId(discordId: Long): Result<LinkedPlayer?> =
        query("find Discord account $discordId") {
            findWhere(DiscordAccountTable.discordId eq discordId)
        }

    override suspend fun findByTelegramId(telegramId: Long): Result<LinkedPlayer?> =
        query("find Telegram account $telegramId") {
            findWhere(TelegramAccountTable.telegramId eq telegramId)
        }

    override suspend fun link(uuid: UUID, minecraftName: String, account: MessengerAccount): Result<Unit> =
        query("link $account to $uuid") {
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
        }

    override suspend fun deleteByUuid(uuid: UUID): Result<LinkedPlayer?> = query("unlink player $uuid") {
        val player = findWhere(PlayerTable.uuid eq uuid)
        PlayerTable.deleteWhere { PlayerTable.uuid eq uuid }
        player
    }

    override suspend fun unlinkDiscord(discordId: Long): Result<LinkedPlayer?> =
        query("unlink Discord account $discordId") {
            val player = findWhere(DiscordAccountTable.discordId eq discordId) ?: return@query null
            DiscordAccountTable.deleteWhere { DiscordAccountTable.discordId eq discordId }
            if (player.telegram == null) {
                PlayerTable.deleteWhere { PlayerTable.uuid eq player.uuid }
            }
            player
        }

    private companion object {
        val DATABASE_TIMEOUT = 10.seconds
    }
}
