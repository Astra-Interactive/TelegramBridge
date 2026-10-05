package ru.astrainteractive.messagebridge.link.dao.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.astrainteractive.klibs.mikro.exposed.model.DatabaseConfiguration
import ru.astrainteractive.klibs.mikro.exposed.util.connectAsFlow
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.link.dao.internal.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.dao.table.DiscordAccountTable
import ru.astrainteractive.messagebridge.link.dao.table.PlayerTable
import ru.astrainteractive.messagebridge.link.dao.table.TelegramAccountTable
import java.io.File
import java.sql.Connection

internal class LinkDatabaseModule(
    ioScope: CoroutineScope,
    dataFolder: File
) {
    private val databaseConfig = DatabaseConfig {
        defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        defaultMaxAttempts = MAX_ATTEMPTS
        defaultMinRetryDelay = 0
        defaultMaxRetryDelay = 0
    }

    private val databaseFlow: Flow<Database> =
        flowOf(DatabaseConfiguration.H2(dataFolder.resolve("linking").absolutePath))
            .flatMapLatest { databaseConfiguration -> databaseConfiguration.connectAsFlow(databaseConfig) }
            .onEach { database ->
                transaction(database) {
                    SchemaUtils.create(PlayerTable, DiscordAccountTable, TelegramAccountTable)
                }
            }
            .shareIn(ioScope, SharingStarted.Eagerly, 1)

    val linkingDao: LinkingDao = LinkingDaoImpl(databaseFlow)

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
