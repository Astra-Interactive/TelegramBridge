package ru.astrainteractive.messagebridge.link.database.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ru.astrainteractive.astralibs.exposed.model.DatabaseConfiguration
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.link.database.table.LinkedPlayerTable
import java.io.File

interface LinkDatabaseModule {
    val databaseFlow: Flow<Database>
    val lifecycle: Lifecycle

    class Default(
        scope: CoroutineScope,
        dataFolder: File
    ) : LinkDatabaseModule {
        override val databaseFlow: Flow<Database> = flowOf(DatabaseConfiguration.H2("linking"))
            .mapCached(scope) { dbConfig, previous ->
                previous?.connector?.invoke()?.close()
                previous?.run(TransactionManager::closeAndUnregister)

                val database = Database.connect(
                    url = "jdbc:h2:${dataFolder.resolve("${dbConfig.name}.db").absolutePath}${dbConfig.stringArgument}",
                    driver = "org.h2.Driver",
                )
                TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE
                transaction(database) {
                    addLogger(Slf4jSqlDebugLogger)
                    SchemaUtils.create(
                        LinkedPlayerTable
                    )
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
