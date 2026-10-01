package ru.astrainteractive.messagebridge.messenger.discord.di

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.setup.BindCodes
import ru.astrainteractive.messagebridge.messaging.setup.DiagnosticCheck
import ru.astrainteractive.messagebridge.messaging.setup.DiscordSetup
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.JdaBuilderFactory
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordBindHandler
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordConnector
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordDiagnostics
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordPermissions
import ru.astrainteractive.messagebridge.messenger.discord.model.awaitJda
import kotlin.time.Duration.Companion.seconds

class JdaMessengerModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    onlinePlayersProvider: OnlinePlayersProvider
) : Logger by JUtiltLogger("MessageBridge-JdaMessengerModule") {
    private val translation by coreModule.translationKrate

    private val failureMapper = DiscordFailureMapper()

    private val connector = DiscordConnector(
        jdaBuilderFactory = JdaBuilderFactory(),
        failureMapper = failureMapper,
    )

    /** Makes the bot connect again with the same settings, e.g. after Message Content Intent is turned on. */
    private val reconnectRequests = MutableStateFlow(0)

    private val connection: StateFlow<DiscordConnection> = combine(
        flow = coreModule.configKrate.cachedStateFlow
            .map { pluginConfiguration -> pluginConfiguration.jdaConfig.copy(channelId = "") }
            .distinctUntilChanged(),
        flow2 = reconnectRequests,
        transform = { jdaConfig, _ -> jdaConfig }
    )
        .flatMapLatest(connector::connect)
        .distinctUntilChanged()
        .onEach(::logConnection)
        .stateIn(coreModule.ioScope, SharingStarted.Eagerly, DiscordConnection.Connecting)

    private val jdaFlow: Flow<JDA?> = connection
        .map { connection -> (connection as? DiscordConnection.Connected)?.jda }
        .distinctUntilChanged()

    private val deliveryError = DiscordDeliveryError(
        failureMapper = failureMapper,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
    )

    private val webhookClient = combine(
        flow = jdaFlow,
        flow2 = coreModule.configKrate.cachedStateFlow.map { it.jdaConfig.channelId }.distinctUntilChanged(),
        transform = ::createWebhookClient
    ).flatMapLatest { webhookClientFlow -> webhookClientFlow }.shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    private val channelProvider = DiscordChannelProvider(
        connection = connection,
        webHookClientFlow = webhookClient,
        configKrate = coreModule.configKrate,
    )

    private val discordMessageController = DiscordBEventConsumer(
        channelProvider = channelProvider,
        topicUpdater = DiscordTopicUpdater(onlinePlayersProvider),
        embedMapper = DiscordEmbedMapper(),
        memberResolver = DiscordMemberResolver(linkModule.linkingDao),
        webhookMessageMapper = DiscordWebhookMessageMapper(),
        failureMapper = failureMapper,
        delivery = deliveryError,
        configKrate = coreModule.configKrate,
    )

    private val relevanceMapper = DiscordMessageRelevanceMapper(
        configKrate = coreModule.configKrate,
    )

    private val commandMapper = DiscordCommandMapper()

    private val messageSender = DiscordMessageSender()

    private val commandHandler = DiscordCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = onlinePlayersProvider,
        linkApi = linkModule.linkApi,
        channelProvider = channelProvider,
        translationKrate = coreModule.translationKrate,
    )

    private val bindCodes = BindCodes()

    private val bindHandler = DiscordBindHandler(
        bindCodes = bindCodes,
        configKrate = coreModule.configKrate,
        messageSender = messageSender,
        translationKrate = coreModule.translationKrate,
    )

    private val messageEventListener = MessageEventListener(
        relevanceMapper = relevanceMapper,
        commandMapper = commandMapper,
        commandHandler = commandHandler,
        replyMapper = DiscordReplyMapper(),
        bindHandler = bindHandler,
        linkApi = linkModule.linkApi,
    )

    private val diagnostics = DiscordDiagnostics(
        connection = connection,
        failureMapper = failureMapper,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
    )

    val setup: DiscordSetup = object : DiscordSetup {
        override val status: StateFlow<MessengerStatus> = connection
            .map(::toStatus)
            .stateIn(coreModule.ioScope, SharingStarted.Eagerly, MessengerStatus.Connecting)

        override val deliveryError: StateFlow<LocalizableComponent?> = discordMessageController.deliveryError

        override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): String = bindCodes.issue(onBound)

        override suspend fun diagnose(): List<DiagnosticCheck> = diagnostics.diagnose()

        override suspend fun inviteUrl(): String? {
            return connection.awaitJda(INVITE_CONNECTION_WAIT)?.getInviteUrl(DiscordPermissions.ALL)
        }
    }

    private fun createWebhookClient(jda: JDA?, channelId: String): Flow<Result<WebhookClient>?> {
        if (jda == null || channelId.isBlank()) return flowOf(null)
        return callbackFlow<Result<WebhookClient>?> {
            val webhookClient = WebHookClientFactory(jda).create(channelId)
            deliveryError.clear()
            send(Result.success(webhookClient))
            awaitClose {
                webhookClient.close()
            }
        }.retryWhen { t, _ ->
            if (t is CancellationException) return@retryWhen false
            deliveryError.report(t)
            emit(Result.failure(t))
            delay(WEBHOOK_RETRY_DELAY)
            true
        }.onStart { emit(null) }
    }

    private fun toStatus(connection: DiscordConnection): MessengerStatus = when (connection) {
        DiscordConnection.Disabled -> MessengerStatus.Disabled
        DiscordConnection.Connecting -> MessengerStatus.Connecting
        is DiscordConnection.Connected -> MessengerStatus.Connected(connection.jda.selfUser.name)
        is DiscordConnection.Failed -> MessengerStatus.Failed(
            reason = failureMapper.toText(connection.failure, translation.discord)
        )
    }

    private fun logConnection(connection: DiscordConnection) {
        when (connection) {
            DiscordConnection.Disabled -> {
                info { "Discord is turned off: the bot token is empty" }
                info { translation.discord.guide.toMessengerText() }
            }

            DiscordConnection.Connecting -> verbose { "#connection connecting to Discord" }
            is DiscordConnection.Connected -> info { "Discord bot ${connection.jda.selfUser.name} is connected" }
            is DiscordConnection.Failed -> error {
                "Discord bot is not connected: " +
                    failureMapper.toText(connection.failure, translation.discord).toMessengerText()
            }
        }
    }

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            jdaFlow
                .filterNotNull()
                .onEach { jda -> messageEventListener.onEnable(jda) }
                .launchIn(coreModule.ioScope)
        },
        onReload = {
            if (connection.value is DiscordConnection.Failed) reconnectRequests.update { requests -> requests + 1 }
        },
        onDisable = {
            discordMessageController.cancel()
            messageEventListener.cancel()
            (connection.value as? DiscordConnection.Connected)?.jda?.let(messageEventListener::onDisable)
        }
    )

    private companion object {
        val WEBHOOK_RETRY_DELAY = 30.seconds
        val INVITE_CONNECTION_WAIT = 10.seconds
    }
}
