@file:Suppress("TrivialLambdaReference")

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
import ru.astrainteractive.messagebridge.messenger.api.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.api.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.api.discord.model.DiscordRoleChange
import ru.astrainteractive.messagebridge.messenger.discord.connection.DiscordChannelFactory
import ru.astrainteractive.messagebridge.messenger.discord.connection.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.connection.JdaSessionFactory
import ru.astrainteractive.messagebridge.messenger.discord.connection.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.listener.DiscordEvents
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.message.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.message.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.message.mapping.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.relay.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.relay.DiscordRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.discord.role.DiscordRoleUpdater
import ru.astrainteractive.messagebridge.messenger.discord.topic.DiscordTopicUpdater

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

    private val relayedMessageCache = DiscordRelayedMessageCache(
        configKrate = coreModule.configKrate,
    )

    private val commandHandler = DiscordCommandHandler(
        messageSender = messageSender,
        platformServer = coreModule.platformServer,
        translationKrate = coreModule.translationKrate,
    )

    private val messageEventListener = MessageEventListener(
        relevanceMapper = relevanceMapper,
        commandMapper = commandMapper,
        commandHandler = commandHandler,
        replyMapper = DiscordReplyMapper(
            translationKrate = coreModule.translationKrate,
            relayedMessageCache = relayedMessageCache,
        ),
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
            clock = coreModule.clock,
            translationKrate = coreModule.translationKrate,
        ),
        embedMapper = DiscordEmbedMapper(coreModule.translationKrate),
        memberResolver = DiscordMemberResolver(authorResolver),
        webhookMessageMapper = DiscordWebhookMessageMapper(coreModule.configKrate, coreModule.translationKrate),
        relayedMessageCache = relayedMessageCache,
        translationKrate = coreModule.translationKrate,
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
