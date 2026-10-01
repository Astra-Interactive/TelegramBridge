package ru.astrainteractive.messagebridge.link.di

import ru.astrainteractive.messagebridge.core.api.api.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.DiscordMembership
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.Unlinking
import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.database.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.player.di.LinkDatabaseModule
import ru.astrainteractive.messagebridge.link.role.internal.DiscordLinkRole
import ru.astrainteractive.messagebridge.link.role.internal.LuckPermsGroups
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

    val unlinking: Unlinking = linkApiImpl
}
