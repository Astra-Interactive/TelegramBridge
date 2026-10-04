package ru.astrainteractive.messagebridge.link.di

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.astralibs.util.parseOrWriteIntoDefault
import ru.astrainteractive.klibs.kstorage.api.asStateFlowKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.internal.CodeApiImpl
import ru.astrainteractive.messagebridge.link.api.internal.LinkApiImpl
import ru.astrainteractive.messagebridge.link.command.DiscordLinkInterceptor
import ru.astrainteractive.messagebridge.link.command.LinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.command.TelegramLinkInterceptor
import ru.astrainteractive.messagebridge.link.command.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.controller.DiscordRoleController
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.dao.internal.LinkingDaoImpl
import ru.astrainteractive.messagebridge.link.database.di.LinkDatabaseModule
import ru.astrainteractive.messagebridge.link.event.LinkedMemberLeaveListener
import ru.astrainteractive.messagebridge.link.internal.LinkedDiscordAuthorResolver
import ru.astrainteractive.messagebridge.link.internal.LinkedNameInterceptor
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messaging.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener

interface LinkModule {
    val lifecycle: Lifecycle

    val codeApi: CodeApi
    val linkApi: LinkApi
    val discordRoleController: DiscordRoleController
    val luckPermsRoleController: LuckPermsRoleController
    val linkingDao: LinkingDao
    val telegramLinkInterceptor: MessageInterceptor<Update>
    val discordLinkInterceptor: MessageInterceptor<MessageReceivedEvent>
    val discordMemberLeaveListener: DiscordMemberLeaveListener
    val discordAuthorResolver: DiscordAuthorResolver
    val linkedNameInterceptor: TextInterceptor

    class Default(
        coreModule: CoreModule,
        luckPermsProvider: LuckPermsProvider
    ) : LinkModule {

        private val linkDatabaseModule = LinkDatabaseModule.Default(
            ioScope = coreModule.ioScope,
            dataFolder = coreModule.dataFolder,
            dispatchers = coreModule.dispatchers
        )

        private val linkTranslationKrate = DefaultMutableKrate(
            factory = ::LinkTranslation,
            loader = {
                coreModule.yamlStringFormat.parseOrWriteIntoDefault(
                    file = coreModule.dataFolder.resolve("link.yml"),
                    logger = JUtiltLogger("MessageBridge-link"),
                    default = ::LinkTranslation
                )
            }
        ).asStateFlowKrate()

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

        override val telegramLinkInterceptor: MessageInterceptor<Update> = TelegramLinkInterceptor(
            linkApi = linkApi,
            linkTranslationKrate = linkTranslationKrate
        )

        override val discordLinkInterceptor: MessageInterceptor<MessageReceivedEvent> = DiscordLinkInterceptor(
            linkApi = linkApi,
            linkTranslationKrate = linkTranslationKrate
        )

        override val discordMemberLeaveListener: DiscordMemberLeaveListener = LinkedMemberLeaveListener(
            linkApi = linkApi
        )

        override val discordAuthorResolver: DiscordAuthorResolver = LinkedDiscordAuthorResolver(linkingDao)

        override val linkedNameInterceptor: TextInterceptor = LinkedNameInterceptor(linkingDao)

        private val commandNodes by lazy {
            listOf(
                LinkLiteralArgumentBuilder(
                    executor = LinkCommandExecutor(
                        codeApi = codeApi,
                        linkingDao = linkingDao,
                        translationKrate = coreModule.translationKrate,
                        linkTranslationKrate = linkTranslationKrate
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
                        translationKrate = coreModule.translationKrate,
                        linkTranslationKrate = linkTranslationKrate
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
            },
            onReload = {
                linkTranslationKrate.getValue()
            }
        )
    }
}
