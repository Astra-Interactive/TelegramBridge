package ru.astrainteractive.messagebridge.link.impl.di

import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.api.DiscordMembership
import ru.astrainteractive.messagebridge.link.api.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.di.LinkModule
import ru.astrainteractive.messagebridge.link.api.di.LinkTranslationModule
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.impl.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.impl.command.LinkCommandExecutor
import ru.astrainteractive.messagebridge.link.impl.command.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.impl.command.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.link.impl.command.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.impl.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.impl.player.database.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.impl.player.di.LinkDatabaseModule
import ru.astrainteractive.messagebridge.link.impl.role.internal.LuckPermsGroups
import kotlin.random.Random

class LinkModuleImpl(
    coreModule: CoreModule,
    linkTranslationModule: LinkTranslationModule
) : LinkModule {
    private val linkDatabaseModule = LinkDatabaseModule(coreModule = coreModule)

    private val linkingDaoImpl = LinkingDaoImpl(linkDatabaseModule.databaseFlow)

    private val codeApi = CodeApiImpl(random = Random.Default)

    private val linkApiImpl = LinkApiImpl(
        linkingDao = linkingDaoImpl,
        discordLinkedPlayerDao = linkingDaoImpl,
        codeApi = codeApi,
        permissionGroups = LuckPermsGroups(LuckPermsProvider.Default),
        configFlow = coreModule.config
    )

    private val commandNodes by lazy {
        listOf(
            LinkLiteralArgumentBuilder(
                executor = LinkCommandExecutor(
                    codeApi = codeApi,
                    linkingDao = linkingDaoImpl,
                    translationKrate = coreModule.translationKrate,
                    linkTranslationKrate = linkTranslationModule.translationKrate
                ),
                ioScope = coreModule.ioScope,
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                platformServer = coreModule.platformServer
            ).create(),
            UnlinkLiteralArgumentBuilder(
                executor = UnlinkCommandExecutor(
                    unlinking = linkApiImpl,
                    translationKrate = coreModule.translationKrate,
                    linkTranslationKrate = linkTranslationModule.translationKrate
                ),
                ioScope = coreModule.ioScope,
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                platformServer = coreModule.platformServer
            ).create()
        )
    }

    override val linkingDao: LinkingDao = linkingDaoImpl

    override val linkApi: LinkApi = linkApiImpl

    override val discordMembership: DiscordMembership = linkApiImpl

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.commandRegistrarContext.registerWhenReady(commandNodes, coreModule.unconfinedScope)
        }
    )
}
