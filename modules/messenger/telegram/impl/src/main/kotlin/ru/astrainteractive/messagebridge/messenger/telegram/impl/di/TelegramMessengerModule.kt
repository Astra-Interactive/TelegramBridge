package ru.astrainteractive.messagebridge.messenger.telegram.impl.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okhttp3.Dns
import org.telegram.telegrambots.longpolling.interfaces.BackOff
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.api.tryConsume
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.di.TelegramBotModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.botUserName
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal.TelegramCommandParser
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.api.TelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal.CappedBackOff
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal.TelegramBotConnector
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal.TelegramConnectionProvider
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal.TelegramStatusLogger
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.Ipv4FirstDns
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.LongPollingBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.TelegramConnectionFactory
import ru.astrainteractive.messagebridge.messenger.telegram.impl.internal.TelegramMessageSenderImpl
import ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping.TelegramFailureTextMapperImpl
import ru.astrainteractive.messagebridge.messenger.telegram.impl.network.OkHttpTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.event.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramBEventConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramMessageValidator
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.mapping.TelegramAuthorMapper
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

class TelegramMessengerModule(
    coreModule: CoreModule,
    updateInterceptors: () -> List<TelegramUpdateInterceptor>,
    bEventChannel: BEventChannel,
) : TelegramBotModule {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val relayedMessageCache = TelegramRelayedMessageCache(
        capacity = RELAYED_MESSAGE_CACHE_CAPACITY
    )

    private val authorMapper = TelegramAuthorMapper(translationKrate = coreModule.translationKrate)

    private val failureMapper = TelegramFailureMapper(
        configFlow = coreModule.config,
    )

    override val failureTextMapper: TelegramFailureTextMapper = TelegramFailureTextMapperImpl(
        translationKrate = coreModule.translationKrate,
    )

    private val connections: SharedFlow<TelegramConnection> = TelegramConnectionProvider(
        configFlow = coreModule.config,
        connectionFactory = TelegramConnectionFactory(dns = Ipv4FirstDns(delegate = Dns.SYSTEM)),
    ).connections.shareIn(moduleIoScope, SharingStarted.Eagerly, replay = 1)

    override val botApi: TelegramBotApi = OkHttpTelegramBotApi(
        telegramClients = connections
            .map { connection -> connection.tryCast<TelegramConnection.Ready>() }
            .map { ready -> ready?.telegramClient },
        failureMapper = failureMapper,
    )

    override val messageSender: TelegramMessageSender = TelegramMessageSenderImpl(
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
    )

    override val state: StateFlow<TelegramConnectionState> = connector.state

    override val deliveryError: StateFlow<LocalizableComponent?> = bEventConsumer.deliveryError

    private val commandHandler = TelegramCommandHandler(
        messageSender = messageSender,
        platformServer = coreModule.platformServer,
        translationKrate = coreModule.translationKrate,
    )

    private val chatConsumer = TelegramChatConsumer(
        scope = moduleIoScope,
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
        commandParser = TelegramCommandParser(botUserName = { connector.state.value.botUserName }),
        commandHandler = commandHandler,
        messageSender = messageSender,
        eventChannel = bEventChannel,
        updateInterceptors = updateInterceptors,
    )

    private val statusLogger = TelegramStatusLogger(
        translationKrate = coreModule.translationKrate,
        failureTextMapper = failureTextMapper,
        logger = JUtiltLogger("MessageBridge-TelegramModule"),
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            moduleIoScope.launch { connector.connect(connections) }
            connector.state.onEach(statusLogger::log).launchIn(moduleIoScope)
            bEventChannel.bEvents(moduleIoScope)
                .onEach { bEvent -> bEventConsumer.tryConsume(bEvent) }
                .launchIn(moduleIoScope)
        },
        onDisable = { moduleIoScope.cancel() }
    )

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
    )

    private companion object {
        const val SEND_RETRIES = 5
        val SEND_RETRY_DELAY = 500.milliseconds
        val BACK_OFF_INITIAL_INTERVAL = 500.milliseconds
        val BACK_OFF_MAX_INTERVAL = 1.minutes
        const val BACK_OFF_MULTIPLIER = 1.5
        const val BACK_OFF_RANDOMIZATION_FACTOR = 0.5
        const val RELAYED_MESSAGE_CACHE_CAPACITY = 1000
    }
}
