package ru.astrainteractive.messagebridge.link.database.di

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
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.exposed.model.DatabaseConfiguration
import ru.astrainteractive.klibs.mikro.exposed.util.connectAsFlow
import ru.astrainteractive.messagebridge.link.database.table.LinkedPlayerTable
import java.io.File

interface LinkDatabaseModule {
    val databaseFlow: Flow<Database>
    val lifecycle: Lifecycle

    class Default(
        ioScope: CoroutineScope,
        dataFolder: File,
        dispatchers: KotlinDispatchers
    ) : LinkDatabaseModule {
        override val databaseFlow: Flow<Database> =
            flowOf(DatabaseConfiguration.H2(dataFolder.resolve("linking").absolutePath))
                .flatMapLatest { databaseConfiguration -> databaseConfiguration.connectAsFlow() }
                .onEach { database ->
                    TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE
                    transaction(database) {
                        SchemaUtils.create(LinkedPlayerTable)
                    }
                }
                .shareIn(ioScope, SharingStarted.Eagerly, 1)

        override val lifecycle: Lifecycle = Lifecycle.Lambda()
    }
}
