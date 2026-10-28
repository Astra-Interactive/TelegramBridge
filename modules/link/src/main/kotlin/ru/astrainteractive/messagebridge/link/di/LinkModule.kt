package ru.astrainteractive.messagebridge.link.di

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.LinkRoleRepository
import ru.astrainteractive.messagebridge.link.code.api.LinkCodeRepository
import ru.astrainteractive.messagebridge.link.code.memory.InMemoryLinkCodeRepository
import ru.astrainteractive.messagebridge.link.command.DiscordLinkInterceptor
import ru.astrainteractive.messagebridge.link.command.LinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.command.TelegramLinkInterceptor
import ru.astrainteractive.messagebridge.link.command.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.link.command.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.link.event.LinkedDiscordAuthorResolver
import ru.astrainteractive.messagebridge.link.event.LinkedMemberLeaveListener
import ru.astrainteractive.messagebridge.link.event.LinkedNameInterceptor
import ru.astrainteractive.messagebridge.link.usecase.GrantLinkedDiscordRoleUseCase
import ru.astrainteractive.messagebridge.link.usecase.LinkAccountUseCase
import ru.astrainteractive.messagebridge.link.usecase.RevokeLinkedDiscordRoleUseCase
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.api.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.api.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.api.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordRoleChange
import java.util.UUID

class LinkModule(
    coreModule: CoreModule,
    addLinkRole: (UUID) -> Unit,
    removeLinkedRole: (UUID) -> Unit
) {
    private val linkRoleRepository = object : LinkRoleRepository {
        override fun addLinkRole(uuid: UUID) = addLinkRole.invoke(uuid)

        override fun removeLinkedRole(uuid: UUID) = removeLinkedRole.invoke(uuid)
    }

    private val linkDatabaseModule = LinkDatabaseModule(
        ioScope = coreModule.ioScope,
        dataFolder = coreModule.dataFolder
    )

    private val linkedPlayerRepository = linkDatabaseModule.linkedPlayerRepository

    private val linkCodeRepository: LinkCodeRepository = InMemoryLinkCodeRepository()

    private val linkAccountUseCase = LinkAccountUseCase(
        linkCodeRepository = linkCodeRepository,
        linkedPlayerRepository = linkedPlayerRepository,
        linkRoleRepository = linkRoleRepository
    )

    private val discordRoleChannel = Channel<DiscordRoleChange>(Channel.UNLIMITED)

    private val grantLinkedDiscordRoleUseCase = GrantLinkedDiscordRoleUseCase(
        configKrate = coreModule.configKrate,
        roleChanges = discordRoleChannel
    )

    private val revokeLinkedDiscordRoleUseCase = RevokeLinkedDiscordRoleUseCase(
        configKrate = coreModule.configKrate,
        roleChanges = discordRoleChannel
    )

    val discordRoleChanges: Flow<DiscordRoleChange> = discordRoleChannel.receiveAsFlow()

    val telegramLinkInterceptor: MessageInterceptor<Update> = TelegramLinkInterceptor(
        linkAccountUseCase = linkAccountUseCase,
        translationKrate = coreModule.translationKrate
    )

    val discordLinkInterceptor: MessageInterceptor<MessageReceivedEvent> = DiscordLinkInterceptor(
        linkAccountUseCase = linkAccountUseCase,
        grantLinkedDiscordRoleUseCase = grantLinkedDiscordRoleUseCase,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate
    )

    val discordMemberLeaveListener: DiscordMemberLeaveListener = LinkedMemberLeaveListener(
        linkedPlayerRepository = linkedPlayerRepository,
        linkRoleRepository = linkRoleRepository
    )

    val discordAuthorResolver: DiscordAuthorResolver = LinkedDiscordAuthorResolver(linkedPlayerRepository)

    val linkedNameInterceptor: TextInterceptor = LinkedNameInterceptor(linkedPlayerRepository)

    private val commandNodes by lazy {
        listOf(
            LinkLiteralArgumentBuilder(
                executor = LinkCommandExecutor(
                    linkCodeRepository = linkCodeRepository,
                    linkedPlayerRepository = linkedPlayerRepository,
                    translationKrate = coreModule.translationKrate
                ),
                ioScope = coreModule.ioScope,
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                platformServer = coreModule.platformServer
            ).create(),
            UnlinkLiteralArgumentBuilder(
                executor = UnlinkCommandExecutor(
                    linkedPlayerRepository = linkedPlayerRepository,
                    linkRoleRepository = linkRoleRepository,
                    revokeLinkedDiscordRoleUseCase = revokeLinkedDiscordRoleUseCase,
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
