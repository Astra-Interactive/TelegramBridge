package ru.astrainteractive.messagebridge.link.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ru.astrainteractive.astralibs.exposed.factory.DatabaseFactory
import ru.astrainteractive.astralibs.exposed.model.DatabaseConfiguration
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.util.FlowExt.mapCached
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.api.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.controller.DiscordRoleController
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.dao.internal.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.database.table.LinkedPlayerTable

interface LinkModule {
    val lifecycle: Lifecycle

    val codeApi: CodeApi
    val linkApi: LinkApi
    val discordRoleController: DiscordRoleController
    val luckPermsRoleController: LuckPermsRoleController
    val databaseFlow: Flow<Database>
    val linkingDao: LinkingDao

    class Default(
        coreModule: CoreModule
    ) : LinkModule {
        override val databaseFlow: Flow<Database> = flowOf(DatabaseConfiguration.H2("linking"))
            .mapCached(coreModule.scope) { dbConfig, previous ->
                previous?.connector?.invoke()?.close()
                previous?.run(TransactionManager::closeAndUnregister)
                val database = DatabaseFactory(coreModule.plugin.dataFolder).create(dbConfig)
                TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE
                transaction(database) {
                    addLogger(Slf4jSqlDebugLogger)
                    SchemaUtils.create(
                        LinkedPlayerTable
                    )
                }
                database
            }
        override val linkingDao: LinkingDao = LinkingDaoImpl(databaseFlow)
        override val codeApi: CodeApi = CodeApiImpl()
        override val discordRoleController: DiscordRoleController = DiscordRoleController(coreModule.configKrate)
        override val luckPermsRoleController = LuckPermsRoleController(coreModule.configKrate)
        override val linkApi: LinkApi = LinkApiImpl(
            linkingDao = linkingDao,
            codeApi = codeApi,
            discordRoleController = discordRoleController,
            luckPermsRoleController = luckPermsRoleController
        )

        override val lifecycle: Lifecycle = Lifecycle.Lambda(
            onDisable = {
                runBlocking { TransactionManager.closeAndUnregister(databaseFlow.first()) }
            }
        )
    }
}
