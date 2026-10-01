package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.DiscordWebhookClients
import ru.astrainteractive.messagebridge.messenger.discord.channel.internal.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.command.internal.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.command.internal.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.connection.internal.DiscordSession
import ru.astrainteractive.messagebridge.messenger.discord.connection.internal.ExponentialBackoff
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.connection.network.DiscordConnector
import ru.astrainteractive.messagebridge.messenger.discord.connection.network.JdaBuilderFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordMemberSweep
import ru.astrainteractive.messagebridge.messenger.discord.onboarding.internal.DiscordBindHandler
import ru.astrainteractive.messagebridge.messenger.discord.onboarding.internal.DiscordDiagnostics
import ru.astrainteractive.messagebridge.messenger.discord.onboarding.internal.JdaDiscordOnboarding
import ru.astrainteractive.messagebridge.messenger.discord.relay.event.DiscordMessageListener
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.relay.internal.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class JdaMessengerModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    onlinePlayersProvider: OnlinePlayersProvider
) : MessengerOnboardingModule<DiscordOnboarding> {

    private val scope = CoroutineScope(
        context = coreModule.ioScope.coroutineContext + SupervisorJob(coreModule.ioScope.coroutineContext.job)
    )

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
        scope = coreModule.ioScope,
    )

    private val deliveryError = DiscordDeliveryError(
        failureMapper = failureMapper,
        configFlow = coreModule.config,
        translationKrate = coreModule.translationKrate,
    )

    private val webhookClients = DiscordWebhookClients(
        factory = WebHookClientFactory(),
        deliveryError = deliveryError,
    ).clients(
        jdaFlow = session.jda,
        channelIdFlow = coreModule.config.map { config -> config.jdaConfig.channelId }.distinctUntilChanged(),
    ).shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    private val channelProvider = DiscordChannelProvider(
        connection = session.connection,
        webhookClients = webhookClients,
        configFlow = coreModule.config,
    )

    private val consumer = DiscordBEventConsumer(
        channelProvider = channelProvider,
        topicUpdater = DiscordTopicUpdater(
            onlinePlayersProvider = onlinePlayersProvider,
            clock = Clock.System,
            translationKrate = coreModule.translationKrate,
        ),
        embedMapper = DiscordEmbedMapper(translationKrate = coreModule.translationKrate),
        memberResolver = DiscordMemberResolver(linkModule.linkingDao),
        webhookMessageMapper = DiscordWebhookMessageMapper(),
        failureMapper = failureMapper,
        delivery = deliveryError,
        configFlow = coreModule.config,
        scope = scope,
        translationKrate = coreModule.translationKrate,
    )

    private val messageSender = DiscordMessageSender()

    private val bindCodes = BindCodes(
        clock = Clock.System,
        lifetime = BIND_CODE_LIFETIME,
        random = SecureRandom()
    )

    private val messageListener = DiscordMessageListener(
        relevanceMapper = DiscordMessageRelevanceMapper(configFlow = coreModule.config),
        commandMapper = DiscordCommandMapper(),
        commandHandler = DiscordCommandHandler(
            messageSender = messageSender,
            onlinePlayersProvider = onlinePlayersProvider,
            linkApi = linkModule.linkApi,
            channelProvider = channelProvider,
            translationKrate = coreModule.translationKrate,
        ),
        replyMapper = DiscordReplyMapper(),
        bindHandler = DiscordBindHandler(
            bindCodes = bindCodes,
            configKrate = coreModule.configKrate,
            messageSender = messageSender,
            translationKrate = coreModule.translationKrate,
        ),
        scope = scope,
    )

    private val memberLeaveListener = DiscordMemberLeaveListener(
        channelProvider = channelProvider,
        discordMembership = linkModule.discordMembership,
        scope = scope,
    )

    private val memberSweep = DiscordMemberSweep(
        channelProvider = channelProvider,
        discordMembership = linkModule.discordMembership,
    )

    override val onboarding: DiscordOnboarding = JdaDiscordOnboarding(
        connection = session.connection,
        failureMapper = failureMapper,
        bindCodes = bindCodes,
        diagnostics = DiscordDiagnostics(
            connection = session.connection,
            failureMapper = failureMapper,
            configFlow = coreModule.config,
            translationKrate = coreModule.translationKrate,
        ),
        delivery = deliveryError,
        translationKrate = coreModule.translationKrate,
        scope = coreModule.ioScope,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            consumer.start()
            session.jda
                .filterNotNull()
                .onEach { jda ->
                    jda.addEventListener(messageListener, memberLeaveListener)
                    memberSweep.sweep(jda)
                }
                .launchIn(scope)
        },
        onReload = {
            session.reconnectIfFailed()
        },
        onDisable = {
            scope.cancel()
            (session.connection.value as? DiscordConnection.Connected)
                ?.jda
                ?.removeEventListener(messageListener, memberLeaveListener)
        }
    )

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes
        val RECONNECT_DELAY = 5.seconds
        val MAX_RECONNECT_DELAY = 5.minutes
    }
}
