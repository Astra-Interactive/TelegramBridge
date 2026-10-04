package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
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
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import kotlin.time.Duration.Companion.seconds

class JdaMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
    messageInterceptors: List<MessageInterceptor<MessageReceivedEvent>>,
    authorResolver: DiscordAuthorResolver,
    memberLeaveListeners: List<DiscordMemberLeaveListener>
) : Logger by JUtiltLogger("MessageBridge-JdaMessengerModule").withoutParentHandlers() {
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

    val bEventConsumer: BEventConsumer = discordMessageController

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            discordMessageController.cancel()
            messageEventListener.cancel()
        }
    )

    private fun channelSession(jda: JDA, channelId: String): Flow<DiscordChannel> = callbackFlow<DiscordChannel> {
        val textChannel = jda.getTextChannelById(channelId) ?: error("Could not find channel $channelId")
        val webhookClient = WebHookClientFactory(jda).create(channelId).first()
        send(DiscordChannel.Ready(textChannel = textChannel, webhookClient = webhookClient))
        awaitClose {
            webhookClient.close()
        }
    }.retryWhen { t, _ ->
        error { "#channelSession could not open channel $channelId: ${t.message}" }
        emit(DiscordChannel.Failed)
        delay(RETRY_DELAY)
        t !is CancellationException
    }

    private fun connect(config: PluginConfiguration.JdaConfig): Flow<DiscordChannel> = jdaSessionFactory.create(config)
        .flatMapLatest { jda -> channelSession(jda, config.channelId) }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
