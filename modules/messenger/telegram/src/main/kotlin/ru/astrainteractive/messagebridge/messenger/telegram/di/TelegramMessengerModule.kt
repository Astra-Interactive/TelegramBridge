package ru.astrainteractive.messagebridge.messenger.telegram.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.Dns
import org.telegram.telegrambots.longpolling.interfaces.BackOff
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.api.api.tryConsume
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.telegram.command.internal.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.command.internal.TelegramCommandParser
import ru.astrainteractive.messagebridge.messenger.telegram.connection.api.TelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.connection.internal.CappedBackOff
import ru.astrainteractive.messagebridge.messenger.telegram.connection.internal.TelegramBotConnector
import ru.astrainteractive.messagebridge.messenger.telegram.connection.internal.TelegramConnectionProvider
import ru.astrainteractive.messagebridge.messenger.telegram.connection.network.Ipv4FirstDns
import ru.astrainteractive.messagebridge.messenger.telegram.connection.network.LongPollingBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.connection.network.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.connection.network.TelegramConnectionFactory
import ru.astrainteractive.messagebridge.messenger.telegram.failure.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.failure.mapping.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramBindHandler
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramDiagnostics
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramOnboarding
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramStatusLogger
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramStatusMapper
import ru.astrainteractive.messagebridge.messenger.telegram.relay.event.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramAuthorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramBEventConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramMessageValidator
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.request.internal.OkHttpTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.internal.TelegramMessageSender
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

class TelegramMessengerModule(
    coreModule: CoreModule,
    onlinePlayersProvider: OnlinePlayersProvider,
    linkModule: LinkModule,
) : MessengerOnboardingModule<MessengerOnboarding> {
    private val scope = CoroutineScope(
        coreModule.ioScope.coroutineContext + SupervisorJob(coreModule.ioScope.coroutineContext[Job])
    )

    private val relayedMessageCache = TelegramRelayedMessageCache(
        capacity = RELAYED_MESSAGE_CACHE_CAPACITY
    )

    private val authorMapper = TelegramAuthorMapper(translationKrate = coreModule.translationKrate)

    private val failureMapper = TelegramFailureMapper(
        configFlow = coreModule.config,
    )

    private val failureTextMapper = TelegramFailureTextMapper(
        translationKrate = coreModule.translationKrate,
    )

    private val connections: SharedFlow<TelegramConnection> = TelegramConnectionProvider(
        configFlow = coreModule.config,
        connectionFactory = TelegramConnectionFactory(dns = Ipv4FirstDns(delegate = Dns.SYSTEM)),
    ).connections.shareIn(scope, SharingStarted.Eagerly, replay = 1)

    private val botApi = OkHttpTelegramBotApi(
        telegramClients = connections.map { connection -> (connection as? TelegramConnection.Ready)?.telegramClient },
        failureMapper = failureMapper,
    )

    private val messageSender = TelegramMessageSender(
        botApi = botApi,
        maxRetries = SEND_RETRIES,
        retryDelay = SEND_RETRY_DELAY,
        logger = JUtiltLogger("MessageBridge-TelegramMessageSender"),
    )

    private val bEventConsumer = TelegramBEventConsumer(
        configFlow = coreModule.config,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        botApi = botApi,
        failureTextMapper = failureTextMapper,
        relayedMessageCache = relayedMessageCache,
        logger = JUtiltLogger("MessageBridge-TelegramBEventConsumer"),
    )

    private val connector = TelegramBotConnector(
        sessionFactory = ::createSession,
        backOffFactory = ::createBackOff,
        failureMapper = failureMapper,
        logger = JUtiltLogger("MessageBridge-TelegramConnector"),
    )

    private val bindCodes = BindCodes(
        clock = Clock.System,
        lifetime = BIND_CODE_LIFETIME,
        random = SecureRandom(),
    )

    private val commandHandler = TelegramCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = onlinePlayersProvider,
        linkApi = linkModule.linkApi,
        bindHandler = TelegramBindHandler(
            bindCodes = bindCodes,
            botApi = botApi,
            messageSender = messageSender,
            configKrate = coreModule.configKrate,
            translationKrate = coreModule.translationKrate,
            logger = JUtiltLogger("MessageBridge-TelegramBindHandler"),
        ),
        chatInfoHandler = TelegramChatInfoHandler(
            messageSender = messageSender,
            translationKrate = coreModule.translationKrate,
            logger = JUtiltLogger("MessageBridge-TelegramChatInfoHandler"),
        ),
        translationKrate = coreModule.translationKrate,
    )

    private val chatConsumer = TelegramChatConsumer(
        scope = scope,
        translationKrate = coreModule.translationKrate,
        relevanceMapper = TelegramMessageRelevanceMapper(
            configFlow = coreModule.config,
            clock = Clock.System,
        ),
        validator = TelegramMessageValidator(
            configFlow = coreModule.config,
            authorMapper = authorMapper,
        ),
        replyMapper = TelegramReplyMapper(
            configFlow = coreModule.config,
            authorMapper = authorMapper,
            relayedMessageCache = relayedMessageCache,
        ),
        commandParser = TelegramCommandParser(botUserName = { connector.botUserName }),
        commandHandler = commandHandler,
        messageSender = messageSender,
        eventChannel = BEventChannel,
        logger = JUtiltLogger("MessageBridge-TelegramChatConsumer"),
    )

    private val statusLogger = TelegramStatusLogger(
        translationKrate = coreModule.translationKrate,
        failureTextMapper = failureTextMapper,
        logger = JUtiltLogger("MessageBridge-TelegramModule"),
    )

    private val statusMapper = TelegramStatusMapper(
        failureTextMapper = failureTextMapper,
    )

    private val status: StateFlow<MessengerStatus> = connector.state
        .map(statusMapper::map)
        .stateIn(scope, SharingStarted.Eagerly, MessengerStatus.Connecting)

    override val onboarding: MessengerOnboarding = TelegramOnboarding(
        status = status,
        deliveryError = bEventConsumer.deliveryError,
        bindCodes = bindCodes,
        diagnostics = TelegramDiagnostics(
            configFlow = coreModule.config,
            translationKrate = coreModule.translationKrate,
            connections = connections,
            botApi = botApi,
            failureTextMapper = failureTextMapper,
        ),
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = { scope.cancel() }
    )

    init {
        scope.launch { connector.connect(connections) }
        connector.state.onEach(statusLogger::log).launchIn(scope)
        BEventChannel.bEvents(scope)
            .onEach { bEvent -> bEventConsumer.tryConsume(bEvent) }
            .launchIn(scope)
    }

    private fun createBackOff(): BackOff = CappedBackOff(
        initialInterval = BACK_OFF_INITIAL_INTERVAL,
        maxInterval = BACK_OFF_MAX_INTERVAL,
        multiplier = BACK_OFF_MULTIPLIER,
        randomizationFactor = BACK_OFF_RANDOMIZATION_FACTOR,
        random = Random.Default,
    )

    private fun createSession(connection: TelegramConnection.Ready): TelegramBotSession = LongPollingBotSession(
        connection = connection,
        updateConsumer = chatConsumer,
        failureMapper = failureMapper,
        backOffFactory = ::createBackOff,
        timeSource = TimeSource.Monotonic,
        logger = JUtiltLogger("MessageBridge-TelegramPolling"),
    )

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes
        const val SEND_RETRIES = 5
        val SEND_RETRY_DELAY = 500.milliseconds
        val BACK_OFF_INITIAL_INTERVAL = 500.milliseconds
        val BACK_OFF_MAX_INTERVAL = 1.minutes
        const val BACK_OFF_MULTIPLIER = 1.5
        const val BACK_OFF_RANDOMIZATION_FACTOR = 0.5
        const val RELAYED_MESSAGE_CACHE_CAPACITY = 1000
    }
}
