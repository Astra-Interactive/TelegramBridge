package ru.astrainteractive.messagebridge.link.database.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.coroutines.mapCached
import ru.astrainteractive.klibs.mikro.exposed.model.DatabaseConfiguration
import ru.astrainteractive.klibs.mikro.exposed.util.connect
import ru.astrainteractive.messagebridge.link.database.table.LinkedPlayerTable
import java.io.File

interface LinkDatabaseModule {
    val databaseFlow: Flow<Database>
    val lifecycle: Lifecycle

    class Default(
        scope: CoroutineScope,
        dataFolder: File
    ) : LinkDatabaseModule {
        override val databaseFlow: Flow<Database> = flowOf(
            value = DatabaseConfiguration.H2(dataFolder.resolve("linking").absolutePath)
        ).mapCached(scope) { dbConfig, previous ->
            previous?.connector?.invoke()?.close()
            previous?.run(TransactionManager::closeAndUnregister)
            val database = dbConfig.connect()
            TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE
            transaction(database) {
                SchemaUtils.create(LinkedPlayerTable)
            }
            database
        }

        override val lifecycle: Lifecycle = Lifecycle.Lambda(
            onDisable = {
                GlobalScope.launch { databaseFlow.firstOrNull()?.run(TransactionManager::closeAndUnregister) }
            }
        )
    }
}
