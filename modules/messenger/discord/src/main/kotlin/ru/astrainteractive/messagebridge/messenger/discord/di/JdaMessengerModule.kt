package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.InterfacedEventManager
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.command.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.command.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.DiscordChannelFactory
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.JdaSessionFactory
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordEvents
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordRoleUpdater
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange
import kotlin.time.Clock

class JdaMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
    messageInterceptors: List<MessageInterceptor<MessageReceivedEvent>>,
    authorResolver: DiscordAuthorResolver,
    memberLeaveListeners: List<DiscordMemberLeaveListener>,
    roleChanges: Flow<DiscordRoleChange>
) {
    private val relevanceMapper = DiscordMessageRelevanceMapper(
        configKrate = coreModule.configKrate,
    )

    private val commandMapper = DiscordCommandMapper()

    private val messageSender = DiscordMessageSender()

    private val commandHandler = DiscordCommandHandler(
        messageSender = messageSender,
        platformServer = coreModule.platformServer,
        translationKrate = coreModule.translationKrate,
    )

    private val messageEventListener = MessageEventListener(
        relevanceMapper = relevanceMapper,
        commandMapper = commandMapper,
        commandHandler = commandHandler,
        replyMapper = DiscordReplyMapper(),
        messageSender = messageSender,
        messageInterceptors = messageInterceptors,
        bEventConsumer = bEventChannel,
    )

    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val jdaEventManager = InterfacedEventManager()

    private val discordEvents = DiscordEvents(
        eventManager = jdaEventManager,
        messageEventListener = messageEventListener,
        memberLeaveListeners = memberLeaveListeners,
        ioScope = moduleIoScope,
    )

    private val jdaSessionFactory = JdaSessionFactory(
        configKrate = coreModule.configKrate,
        eventManager = jdaEventManager,
        ioScope = coreModule.ioScope,
    )

    private val discordChannelFactory = DiscordChannelFactory(
        webHookClientFactory = WebHookClientFactory(),
    )

    private val channelProvider = DiscordChannelProvider(
        jdaConfigFlow = coreModule.configKrate.cachedStateFlow
            .map { pluginConfiguration -> pluginConfiguration.jdaConfig },
        connect = { config ->
            jdaSessionFactory.create(config)
                .flatMapLatest { jda -> discordChannelFactory.create(jda, config.channelId) }
        },
        scope = coreModule.ioScope,
    )

    private val discordMessageController = DiscordBEventConsumer(
        discordChannel = channelProvider.channel,
        topicUpdater = DiscordTopicUpdater(
            platformServer = coreModule.platformServer,
            clock = Clock.System,
        ),
        embedMapper = DiscordEmbedMapper(),
        memberResolver = DiscordMemberResolver(authorResolver),
        webhookMessageMapper = DiscordWebhookMessageMapper(),
        bEventReceiver = bEventChannel,
    )

    private val roleUpdater = DiscordRoleUpdater(
        discordChannel = channelProvider.channel,
        roleChanges = roleChanges,
    )

    val bEventConsumer: BEventConsumer = discordMessageController

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            moduleIoScope.cancel()
            discordMessageController.cancel()
            messageEventListener.cancel()
            roleUpdater.cancel()
        }
    )
}
