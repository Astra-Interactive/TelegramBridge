package ru.astrainteractive.messagebridge.link.player.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.astrainteractive.klibs.mikro.exposed.model.DatabaseConfiguration
import ru.astrainteractive.klibs.mikro.exposed.util.connectAsFlow
import ru.astrainteractive.messagebridge.link.player.database.LinkedPlayerTable
import java.io.File
import java.sql.Connection

internal class LinkDatabaseModule(
    ioScope: CoroutineScope,
    dataFolder: File
) {
    val databaseFlow: Flow<Database> = flowOf(DatabaseConfiguration.H2(dataFolder.resolve("linking").absolutePath))
        .flatMapLatest { databaseConfiguration -> databaseConfiguration.connectAsFlow() }
        .onEach { database ->
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(database) {
                SchemaUtils.create(LinkedPlayerTable)
            }
        }
        .shareIn(ioScope, SharingStarted.Eagerly, 1)
}
