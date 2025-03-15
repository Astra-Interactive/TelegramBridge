package ru.astrainteractive.messagebridge.link.database.dao.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.database.table.LinkedPlayerTable
import java.util.UUID

class LinkingDaoImpl(
    private val databaseFlow: Flow<Database>
) : LinkingDao, Logger by JUtiltLogger("LinkingDao") {
    private suspend fun requireDatabase() = databaseFlow.first()

    private fun toLinkedPlayerModel(row: ResultRow): LinkedPlayerModel {
        return LinkedPlayerModel(
            uuid = UUID.fromString(row[LinkedPlayerTable.id].value),
            lastMinecraftName = row[LinkedPlayerTable.lastMinecraftName],
            discordLink = let {
                LinkedPlayerModel.DiscordLink(
                    discordId = row[LinkedPlayerTable.discordId] ?: return@let null,
                    lastDiscordName = row[LinkedPlayerTable.lastDiscordName] ?: return@let null
                )
            },
            telegramLink = let {
                LinkedPlayerModel.TelegramLink(
                    telegramUsername = row[LinkedPlayerTable.lastTelegramName] ?: return@let null,
                    telegramId = row[LinkedPlayerTable.telegramId] ?: return@let null
                )
            }
        )
    }

    override suspend fun findByUuid(uuid: UUID): Result<LinkedPlayerModel?> = kotlin.runCatching {
        transaction(requireDatabase()) {
            LinkedPlayerTable.selectAll()
                .where { LinkedPlayerTable.id eq uuid.toString() }
                .limit(1)
                .map(::toLinkedPlayerModel)
                .firstOrNull()
        }
    }

    override suspend fun findByDiscordId(id: Long): Result<LinkedPlayerModel> = kotlin.runCatching {
        transaction(requireDatabase()) {
            LinkedPlayerTable.selectAll()
                .where { LinkedPlayerTable.discordId eq id }
                .limit(1)
                .map(::toLinkedPlayerModel)
                .first()
        }
    }

    override suspend fun findByTelegramId(id: Long): Result<LinkedPlayerModel> = kotlin.runCatching {
        transaction(requireDatabase()) {
            LinkedPlayerTable.selectAll()
                .where { LinkedPlayerTable.telegramId eq id }
                .limit(1)
                .map(::toLinkedPlayerModel)
                .first()
        }
    }

    override suspend fun upsert(linkedPlayerModel: LinkedPlayerModel): Result<LinkedPlayerModel> = kotlin.runCatching {
        transaction(requireDatabase()) {
            val isExists = LinkedPlayerTable.selectAll()
                .where { LinkedPlayerTable.id eq linkedPlayerModel.uuid.toString() }
                .count() >= 1
            if (isExists) {
                LinkedPlayerTable
                    .update(
                        where = {
                            LinkedPlayerTable.id eq linkedPlayerModel.uuid.toString()
                        },
                        body = {
                            it[LinkedPlayerTable.lastMinecraftName] = linkedPlayerModel.lastMinecraftName
                            it[LinkedPlayerTable.discordId] = linkedPlayerModel.discordLink?.discordId
                            it[LinkedPlayerTable.lastDiscordName] = linkedPlayerModel.discordLink?.lastDiscordName
                            it[LinkedPlayerTable.telegramId] = linkedPlayerModel.telegramLink?.telegramId
                            it[LinkedPlayerTable.lastTelegramName] = linkedPlayerModel.telegramLink?.telegramUsername
                        }
                    )
            } else {
                LinkedPlayerTable.insert {
                    it[LinkedPlayerTable.id] = linkedPlayerModel.uuid.toString()
                    it[LinkedPlayerTable.lastMinecraftName] = linkedPlayerModel.lastMinecraftName
                    it[LinkedPlayerTable.discordId] = linkedPlayerModel.discordLink?.discordId
                    it[LinkedPlayerTable.lastDiscordName] = linkedPlayerModel.discordLink?.lastDiscordName
                    it[LinkedPlayerTable.telegramId] = linkedPlayerModel.telegramLink?.telegramId
                    it[LinkedPlayerTable.lastTelegramName] = linkedPlayerModel.telegramLink?.telegramUsername
                }
            }
        }
        findByUuid(linkedPlayerModel.uuid).getOrThrow() ?: error("Could not insert user somehow?")
    }
}
