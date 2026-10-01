package ru.astrainteractive.messagebridge.link

import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.code.CodeApi
import ru.astrainteractive.messagebridge.link.code.CodeApiImpl
import ru.astrainteractive.messagebridge.link.player.LinkDatabaseModule
import ru.astrainteractive.messagebridge.link.player.LinkingDao
import ru.astrainteractive.messagebridge.link.player.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.role.DiscordLinkRole
import ru.astrainteractive.messagebridge.link.role.LuckPermsGroups
import ru.astrainteractive.messagebridge.link.role.LuckPermsRoleController
import kotlin.random.Random

class LinkModule(
    coreModule: CoreModule,
    luckPermsProvider: LuckPermsProvider
) {
    private val linkDatabaseModule = LinkDatabaseModule(
        ioScope = coreModule.ioScope,
        dataFolder = coreModule.dataFolder
    )

    private val linkingDaoImpl = LinkingDaoImpl(linkDatabaseModule.databaseFlow)

    val linkingDao: LinkingDao = linkingDaoImpl

    val codeApi: CodeApi = CodeApiImpl(random = Random.Default)

    val luckPermsRoleController = LuckPermsRoleController(
        configFlow = coreModule.config,
        luckPermsProvider = luckPermsProvider
    )

    private val linkApiImpl = LinkApiImpl(
        linkingDao = linkingDaoImpl,
        discordLinkedPlayerDao = linkingDaoImpl,
        codeApi = codeApi,
        discordLinkRole = DiscordLinkRole(),
        permissionGroups = LuckPermsGroups(luckPermsProvider),
        configFlow = coreModule.config
    )

    val linkApi: LinkApi = linkApiImpl

    val discordMembership: DiscordMembership = linkApiImpl
}
