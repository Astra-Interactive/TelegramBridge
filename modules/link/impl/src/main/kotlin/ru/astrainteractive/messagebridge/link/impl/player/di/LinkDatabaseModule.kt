package ru.astrainteractive.messagebridge.link.impl.player.di

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
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.impl.player.database.LinkedPlayerTable
import java.sql.Connection

internal class LinkDatabaseModule(
    coreModule: CoreModule
) {
    private val databaseConfiguration = DatabaseConfiguration.H2(coreModule.dataFolder.resolve("linking").absolutePath)

    val databaseFlow: Flow<Database> = flowOf(databaseConfiguration)
        .flatMapLatest { databaseConfiguration -> databaseConfiguration.connectAsFlow() }
        .onEach { database ->
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(database) {
                SchemaUtils.create(LinkedPlayerTable)
            }
        }
        .shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)
}
