package ru.astrainteractive.messagebridge.link.impl.player.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.impl.player.api.DiscordLinkedPlayerDao
import ru.astrainteractive.messagebridge.link.impl.player.model.LinkedPlayerStorageError
import java.util.UUID

internal class LinkingDaoImpl(
    private val databaseFlow: Flow<Database>
) : LinkingDao,
    DiscordLinkedPlayerDao {
    private val linkedPlayers = MinecraftPlayerTable leftJoin DiscordLinkTable leftJoin TelegramLinkTable

    private val discordLinkedPlayers = MinecraftPlayerTable innerJoin DiscordLinkTable leftJoin TelegramLinkTable

    private fun findFirst(where: () -> Op<Boolean>): LinkedPlayerModel? {
        return linkedPlayers.selectAll()
            .where(where)
            .limit(1)
            .map { row -> row.toLinkedPlayerModel() }
            .firstOrNull()
    }

    private fun saveDiscordLink(id: String, discordLink: LinkedPlayerModel.DiscordLink?) {
        if (discordLink == null) {
            DiscordLinkTable.deleteWhere { DiscordLinkTable.uuid eq id }
            return
        }
        DiscordLinkTable.upsert { statement ->
            statement[DiscordLinkTable.uuid] = id
            statement[DiscordLinkTable.discordId] = discordLink.discordId
            statement[DiscordLinkTable.lastDiscordName] = discordLink.lastDiscordName
        }
    }

    private fun saveTelegramLink(id: String, telegramLink: LinkedPlayerModel.TelegramLink?) {
        if (telegramLink == null) {
            TelegramLinkTable.deleteWhere { TelegramLinkTable.uuid eq id }
            return
        }
        TelegramLinkTable.upsert { statement ->
            statement[TelegramLinkTable.uuid] = id
            statement[TelegramLinkTable.telegramId] = telegramLink.telegramId
            statement[TelegramLinkTable.lastTelegramName] = telegramLink.telegramUsername
        }
    }

    private suspend fun <T> query(action: String, statement: JdbcTransaction.() -> T): Result<T> {
        val database = databaseFlow.first()
        return runCatching { transaction(db = database, statement = statement) }
            .propagateCancellationException()
            .fold(
                onSuccess = { value -> Result.success(value) },
                onFailure = { t -> Result.failure(LinkedPlayerStorageError("Could not $action", t)) }
            )
    }

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?> {
        return query("find the player $uuid") {
            findFirst { MinecraftPlayerTable.id eq uuid.toString() }
        }
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<Unit> {
        return query("delete the player $uuid") {
            MinecraftPlayerTable.deleteWhere { MinecraftPlayerTable.id eq uuid.toString() }
            Unit
        }
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel?> {
        return query("find the player with Discord id $id") {
            findFirst { DiscordLinkTable.discordId eq id }
        }
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel?> {
        return query("find the player with Telegram id $id") {
            findFirst { TelegramLinkTable.telegramId eq id }
        }
    }

    override suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>> {
        return query("find the players with a Discord link") {
            discordLinkedPlayers.selectAll()
                .map { row -> row.toLinkedPlayerModel() }
        }
    }

    override suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel> {
        val id = linkedPlayerModel.uuid.toString()
        return query("save the player $id") {
            MinecraftPlayerTable.upsert { statement ->
                statement[MinecraftPlayerTable.id] = id
                statement[MinecraftPlayerTable.lastMinecraftName] = linkedPlayerModel.lastMinecraftName
            }
            saveDiscordLink(id, linkedPlayerModel.discordLink)
            saveTelegramLink(id, linkedPlayerModel.telegramLink)
            linkedPlayerModel
        }
    }
}
