package ru.astrainteractive.messagebridge.link.di

import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.api.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.command.LinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.command.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.UnlinkLiteralArgumentBuilder
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
            ioScope = coreModule.ioScope,
            dataFolder = coreModule.dataFolder,
            dispatchers = coreModule.dispatchers
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

        private val commandNodes by lazy {
            listOf(
                LinkLiteralArgumentBuilder(
                    executor = LinkCommandExecutor(
                        codeApi = codeApi,
                        linkingDao = linkingDao,
                        translationKrate = coreModule.translationKrate
                    ),
                    ioScope = coreModule.ioScope,
                    multiplatformCommand = coreModule.multiplatformCommand,
                    commandExceptionHandler = coreModule.commandExceptionHandler,
                    platformServer = coreModule.platformServer
                ).create(),
                UnlinkLiteralArgumentBuilder(
                    executor = UnlinkCommandExecutor(
                        linkingDao = linkingDao,
                        luckPermsRoleController = luckPermsRoleController,
                        translationKrate = coreModule.translationKrate
                    ),
                    ioScope = coreModule.ioScope,
                    multiplatformCommand = coreModule.multiplatformCommand,
                    commandExceptionHandler = coreModule.commandExceptionHandler,
                    platformServer = coreModule.platformServer
                ).create()
            )
        }

        override val lifecycle: Lifecycle = Lifecycle.Lambda(
            onEnable = {
                coreModule.commandRegistrarContext.registerWhenReady(commandNodes, coreModule.unconfinedScope)
            }
        )
    }
}
