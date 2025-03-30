package ru.astrainteractive.messagebridge.link.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.api.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.controller.DiscordRoleController
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.dao.internal.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.database.di.LinkDatabaseModule

interface LinkModule {
    val lifecycle: Lifecycle

    val codeApi: CodeApi
    val linkApi: LinkApi
    val discordRoleController: DiscordRoleController
    val luckPermsRoleController: LuckPermsRoleController
    val linkingDao: LinkingDao

    class Default(
        coreModule: CoreModule,
        luckPermsProvider: LuckPermsProvider
    ) : LinkModule {

        private val linkDatabaseModule = LinkDatabaseModule.Default(
            scope = coreModule.scope,
            dataFolder = coreModule.dataFolder
        )

        override val linkingDao: LinkingDao = LinkingDaoImpl(linkDatabaseModule.databaseFlow)
        override val codeApi: CodeApi = CodeApiImpl()
        override val discordRoleController: DiscordRoleController = DiscordRoleController(coreModule.configKrate)
        override val luckPermsRoleController = LuckPermsRoleController(
            configKrate = coreModule.configKrate,
            luckPermsProvider = luckPermsProvider
        )
        override val linkApi: LinkApi = LinkApiImpl(
            linkingDao = linkingDao,
            codeApi = codeApi,
            discordRoleController = discordRoleController,
            luckPermsRoleController = luckPermsRoleController
        )

        override val lifecycle: Lifecycle = Lifecycle.Lambda()
    }
}
