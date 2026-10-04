package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.DiscordChannelFactory
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.JdaSessionFactory
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordRoleUpdater
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange

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

    private val jdaSessionFactory = JdaSessionFactory(
        configKrate = coreModule.configKrate,
        messageEventListener = messageEventListener,
        memberLeaveListeners = memberLeaveListeners,
        ioScope = coreModule.ioScope,
    )

    private val discordChannelFactory = DiscordChannelFactory(
        webHookClientFactory = WebHookClientFactory(),
    )

    private val channelProvider = DiscordChannelProvider(
        jdaConfigFlow = coreModule.configKrate.cachedStateFlow
            .map { pluginConfiguration -> pluginConfiguration.jdaConfig },
        connect = ::connect,
        scope = coreModule.ioScope,
    )

    private val discordMessageController = DiscordBEventConsumer(
        discordChannel = channelProvider.channel,
        topicUpdater = DiscordTopicUpdater(coreModule.platformServer),
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
            discordMessageController.cancel()
            messageEventListener.cancel()
            roleUpdater.cancel()
        }
    )

    private fun connect(config: PluginConfiguration.JdaConfig): Flow<DiscordChannel> = jdaSessionFactory.create(config)
        .flatMapLatest { jda -> discordChannelFactory.create(jda, config.channelId) }
}
