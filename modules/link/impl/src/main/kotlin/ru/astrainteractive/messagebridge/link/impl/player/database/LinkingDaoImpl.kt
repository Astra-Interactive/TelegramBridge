package ru.astrainteractive.messagebridge.link.impl.player.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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

    private fun findFirst(where: () -> Op<Boolean>): LinkedPlayerModel? {
        return LinkedPlayerTable.selectAll()
            .where(where)
            .limit(1)
            .map { row -> row.toLinkedPlayerModel() }
            .firstOrNull()
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
            findFirst { LinkedPlayerTable.id eq uuid.toString() }
        }
    }

    override suspend fun deleteByUuid(uuid: UUID): Result<Unit> {
        return query("delete the player $uuid") {
            LinkedPlayerTable.deleteWhere { LinkedPlayerTable.id eq uuid.toString() }
            Unit
        }
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel?> {
        return query("find the player with Discord id $id") {
            findFirst { LinkedPlayerTable.discordId eq id }
        }
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel?> {
        return query("find the player with Telegram id $id") {
            findFirst { LinkedPlayerTable.telegramId eq id }
        }
    }

    override suspend fun findAllWithDiscordLink(): Result<List<LinkedPlayerModel>> {
        return query("find the players with a Discord link") {
            LinkedPlayerTable.selectAll()
                .where { LinkedPlayerTable.discordId.isNotNull() }
                .map { row -> row.toLinkedPlayerModel() }
                .filter { linkedPlayer -> linkedPlayer.discordLink != null }
        }
    }

    override suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel> {
        val id = linkedPlayerModel.uuid.toString()
        return query("save the player $id") {
            val isStored = LinkedPlayerTable.selectAll()
                .where { LinkedPlayerTable.id eq id }
                .count() > 0
            if (isStored) {
                LinkedPlayerTable.update(where = { LinkedPlayerTable.id eq id }) { statement ->
                    statement.writeLinks(linkedPlayerModel)
                }
            } else {
                LinkedPlayerTable.insert { statement ->
                    statement[LinkedPlayerTable.id] = id
                    statement.writeLinks(linkedPlayerModel)
                }
            }
            linkedPlayerModel
        }
    }
}
