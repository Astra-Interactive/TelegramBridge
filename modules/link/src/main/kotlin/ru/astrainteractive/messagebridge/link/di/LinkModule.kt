package ru.astrainteractive.messagebridge.link.di

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.code.api.CodeApi
import ru.astrainteractive.messagebridge.link.code.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.command.DiscordLinkInterceptor
import ru.astrainteractive.messagebridge.link.command.LinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.command.TelegramLinkInterceptor
import ru.astrainteractive.messagebridge.link.command.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.event.LinkedMemberLeaveListener
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.internal.LinkedDiscordAuthorResolver
import ru.astrainteractive.messagebridge.link.internal.LinkedNameInterceptor
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.player.di.LinkDatabaseModule
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener

class LinkModule(
    coreModule: CoreModule,
    luckPermsProvider: LuckPermsProvider
) {
    private val linkDatabaseModule = LinkDatabaseModule(
        ioScope = coreModule.ioScope,
        dataFolder = coreModule.dataFolder
    )

    private val linkingDao = linkDatabaseModule.linkingDao

    private val codeApi: CodeApi = CodeApiImpl()

    private val luckPermsRoleController = LuckPermsRoleController(
        configKrate = coreModule.configKrate,
        luckPermsProvider = luckPermsProvider
    )

    private val linkApi: LinkApi = LinkApiImpl(
        linkingDao = linkingDao,
        codeApi = codeApi,
        discordRoleController = DiscordRoleController(coreModule.configKrate),
        luckPermsRoleController = luckPermsRoleController
    )

    val telegramLinkInterceptor: MessageInterceptor<Update> = TelegramLinkInterceptor(
        linkApi = linkApi,
        translationKrate = coreModule.translationKrate
    )

    val discordLinkInterceptor: MessageInterceptor<MessageReceivedEvent> = DiscordLinkInterceptor(
        linkApi = linkApi,
        translationKrate = coreModule.translationKrate
    )

    val discordMemberLeaveListener: DiscordMemberLeaveListener = LinkedMemberLeaveListener(
        linkApi = linkApi
    )

    val discordAuthorResolver: DiscordAuthorResolver = LinkedDiscordAuthorResolver(linkingDao)

    val linkedNameInterceptor: TextInterceptor = LinkedNameInterceptor(linkingDao)

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

    val lifecycle: Lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.commandRegistrarContext.registerWhenReady(commandNodes, coreModule.unconfinedScope)
        }
    )
}
