package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordMessageSenderImpl
import ru.astrainteractive.messagebridge.messenger.discord.channel.network.DiscordWebhookClients
import ru.astrainteractive.messagebridge.messenger.discord.channel.network.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.command.internal.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.command.internal.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.connection.internal.DiscordSession
import ru.astrainteractive.messagebridge.messenger.discord.connection.internal.ExponentialBackoff
import ru.astrainteractive.messagebridge.messenger.discord.connection.network.DiscordConnector
import ru.astrainteractive.messagebridge.messenger.discord.connection.network.JdaBuilderFactory
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureTextMapperImpl
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.relay.event.DiscordMessageListener
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordWebhookMessageMapper
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class JdaMessengerModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    messageInterceptors: () -> List<DiscordMessageInterceptor>,
    bEventChannel: BEventChannel
) {

    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val failureMapper = DiscordFailureMapper()

    private val session = DiscordSession(
        connector = DiscordConnector(
            jdaBuilderFactory = JdaBuilderFactory(),
            failureMapper = failureMapper,
            backoff = ExponentialBackoff(initial = RECONNECT_DELAY, max = MAX_RECONNECT_DELAY),
        ),
        failureMapper = failureMapper,
        configFlow = coreModule.config,
        translationKrate = coreModule.translationKrate,
        scope = moduleIoScope,
    )

    private val delivery = DiscordDeliveryError(
        failureMapper = failureMapper,
        configFlow = coreModule.config,
        translationKrate = coreModule.translationKrate,
    )

    private val webhookClients = DiscordWebhookClients(
        factory = WebHookClientFactory(),
        deliveryError = delivery,
    ).clients(
        jdaFlow = session.jda,
        channelIdFlow = coreModule.config
            .map { config -> config.jdaConfig }
            .map { jdaConfig -> jdaConfig.channelId }
            .distinctUntilChanged(),
    ).shareIn(moduleIoScope, SharingStarted.Eagerly, 1)

    private val channelProvider = DiscordChannelProvider(
        connection = session.connection,
        webhookClients = webhookClients,
        configFlow = coreModule.config,
    )

    private val consumer = DiscordBEventConsumer(
        channelProvider = channelProvider,
        topicUpdater = DiscordTopicUpdater(
            platformServer = coreModule.platformServer,
            clock = Clock.System,
            translationKrate = coreModule.translationKrate,
        ),
        embedMapper = DiscordEmbedMapper(translationKrate = coreModule.translationKrate),
        memberResolver = DiscordMemberResolver(linkModule.linkingDao),
        webhookMessageMapper = DiscordWebhookMessageMapper(),
        failureMapper = failureMapper,
        delivery = delivery,
        configFlow = coreModule.config,
        scope = moduleIoScope,
        translationKrate = coreModule.translationKrate,
        bEventChannel = bEventChannel,
    )

    val connection: StateFlow<DiscordConnection> = session.connection

    val deliveryError: StateFlow<LocalizableComponent?> = delivery.text

    val messageSender: DiscordMessageSender = DiscordMessageSenderImpl()

    val failureTextMapper: DiscordFailureTextMapper = DiscordFailureTextMapperImpl(
        failureMapper = failureMapper,
        configFlow = coreModule.config,
        translationKrate = coreModule.translationKrate,
    )

    private val messageListener = DiscordMessageListener(
        relevanceMapper = DiscordMessageRelevanceMapper(configFlow = coreModule.config),
        commandMapper = DiscordCommandMapper(),
        commandHandler = DiscordCommandHandler(
            messageSender = messageSender,
            platformServer = coreModule.platformServer,
            translationKrate = coreModule.translationKrate,
        ),
        replyMapper = DiscordReplyMapper(),
        messageInterceptors = messageInterceptors,
        scope = moduleIoScope,
        bEventChannel = bEventChannel,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            consumer.start()
            session.jda
                .filterNotNull()
                .onEach { jda -> jda.addEventListener(messageListener) }
                .launchIn(moduleIoScope)
        },
        onReload = {
            session.reconnectIfFailed()
        },
        onDisable = {
            moduleIoScope.cancel()
            session.connection.value.tryCast<DiscordConnection.Connected>()
                ?.jda
                ?.removeEventListener(messageListener)
        }
    )

    private companion object {
        val RECONNECT_DELAY = 5.seconds
        val MAX_RECONNECT_DELAY = 5.minutes
    }
}
