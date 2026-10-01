package ru.astrainteractive.messagebridge.messenger.telegram.di

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import okhttp3.Dispatcher
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator
import org.telegram.telegrambots.meta.api.methods.GetMe
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.factory.TelegramConnectionFactory
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramAuthorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramBEventConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnectionSettings
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.setup.TelegramDiagnostics
import ru.astrainteractive.messagebridge.messenger.telegram.util.CappedBackOff
import ru.astrainteractive.messagebridge.messenger.telegram.util.GetUpdatesStatusInterceptor
import ru.astrainteractive.messagebridge.onboarding.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.bind.BindCode
import ru.astrainteractive.messagebridge.onboarding.bind.BindCodes
import ru.astrainteractive.messagebridge.onboarding.check.Check
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class TelegramMessengerModule(
    coreModule: CoreModule,
    onlinePlayersProvider: OnlinePlayersProvider,
    linkModule: LinkModule,
) : MessengerOnboardingModule<MessengerOnboarding>,
    Logger by JUtiltLogger("MessageBridge-TelegramModule") {
    private val translation by coreModule.translationKrate

    private val connectionState = MutableStateFlow<TelegramConnectionState>(TelegramConnectionState.Connecting)

    @Volatile
    private var botUserName: String? = null

    private val relayedMessageCache = TelegramRelayedMessageCache(
        capacity = RELAYED_MESSAGE_CACHE_CAPACITY
    )

    private val failureMapper = TelegramFailureMapper(
        configFlow = coreModule.config,
    )

    private val failureTextMapper = TelegramFailureTextMapper(
        translationKrate = coreModule.translationKrate,
    )

    private val connectionFactory = TelegramConnectionFactory()

    private val connectionFlow: SharedFlow<TelegramConnection> = coreModule.config
        .map { configuration -> TelegramConnectionSettings.of(configuration.tgConfig) }
        .distinctUntilChanged()
        .flatMapLatest { settings ->
            callbackFlow {
                val connection = connectionFactory.create(settings)
                send(connection)
                awaitClose { (connection as? TelegramConnection.Ready)?.close() }
            }
        }
        .shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    /** Emits `null` right away when the settings cannot connect, e.g. the token is empty, so senders do not wait. */
    private val telegramClientFlow: Flow<OkHttpTelegramClient?> = connectionFlow
        .map { connection -> (connection as? TelegramConnection.Ready)?.telegramClient }

    private val messageSender = TelegramMessageSender(
        telegramClientFlow = telegramClientFlow,
        failureMapper = failureMapper,
    )

    private val telegramMessageController = TelegramBEventConsumer(
        configFlow = coreModule.config,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        telegramClientFlow = telegramClientFlow,
        failureMapper = failureMapper,
        failureTextMapper = failureTextMapper,
        relayedMessageCache = relayedMessageCache,
    )

    private val authorMapper = TelegramAuthorMapper()

    private val replyMapper = TelegramReplyMapper(
        configFlow = coreModule.config,
        authorMapper = authorMapper,
        relayedMessageCache = relayedMessageCache,
    )

    private val relevanceChecker = TelegramMessageRelevanceMapper(
        configFlow = coreModule.config,
    )

    private val messageValidator = TelegramMessageValidatorMapper(
        configFlow = coreModule.config,
        authorMapper = authorMapper,
    )

    private val commandParser = TelegramCommandMapper(
        botUserName = { botUserName },
    )

    private val bindCodes = BindCodes(
        clock = Clock.System,
        lifetime = BIND_CODE_LIFETIME,
        random = SecureRandom()
    )

    private val commandHandler = TelegramCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = onlinePlayersProvider,
        linkApi = linkModule.linkApi,
        bindCodes = bindCodes,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
    )

    private val consumer = TelegramChatConsumer(
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        translationKrate = coreModule.translationKrate,
        relevanceChecker = relevanceChecker,
        validator = messageValidator,
        replyMapper = replyMapper,
        commandParser = commandParser,
        commandHandler = commandHandler,
        messageSender = messageSender,
    )

    private val diagnostics = TelegramDiagnostics(
        configFlow = coreModule.config,
        translationKrate = coreModule.translationKrate,
        connectionFlow = connectionFlow,
        failureMapper = failureMapper,
        failureTextMapper = failureTextMapper,
    )

    private val bridgeBotJob = connectionFlow
        .flatMapLatest { connection ->
            botUserName = null
            when (connection) {
                TelegramConnection.Disabled -> flow { connectionState.value = TelegramConnectionState.Disabled }
                is TelegramConnection.Invalid -> flow {
                    connectionState.value = TelegramConnectionState.Failed(connection.failure)
                }

                is TelegramConnection.Ready -> pollingFlow(connection)
                    .onStart { connectionState.value = TelegramConnectionState.Connecting }
            }
        }
        .launchIn(coreModule.ioScope)

    private val connectionStateLogJob = connectionState
        .onEach(::log)
        .launchIn(coreModule.ioScope)

    private val status: StateFlow<MessengerStatus> = connectionState
        .map(::toMessengerStatus)
        .stateIn(coreModule.ioScope, SharingStarted.Eagerly, MessengerStatus.Connecting)

    override val onboarding: MessengerOnboarding = object : MessengerOnboarding {
        override val status: StateFlow<MessengerStatus> = this@TelegramMessengerModule.status
        override val deliveryError: StateFlow<LocalizableComponent?> = telegramMessageController.deliveryError
        override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)
        override suspend fun check(): List<Check> = diagnostics.diagnose()
    }

    /**
     * Checks the token with getMe before polling, so a wrong one is reported once and waits for new settings
     * instead of being retried forever.
     */
    private fun pollingFlow(connection: TelegramConnection.Ready): Flow<Unit> {
        val backOff = CappedBackOff()
        return flow<Unit> {
            val bot = connection.telegramClient.executeAsync(GetMe()).await()
            val botName = "@${bot.userName}"
            botUserName = bot.userName
            connectionState.value = TelegramConnectionState.Connected(botName)
            poll(connection, botName, onRegistered = backOff::reset)
        }.retryWhen { throwable, _ ->
            currentCoroutineContext().ensureActive()
            val failure = failureMapper.map(throwable)
            connectionState.value = TelegramConnectionState.Failed(failure)
            if (failure.needsNewSettings) return@retryWhen false
            val retryDelay = backOff.nextBackOffMillis().milliseconds
            verbose { "#pollingFlow could not connect, retrying in $retryDelay: ${throwable.message}" }
            delay(retryDelay)
            true
        }.catch { awaitCancellation() }
    }

    /**
     * Polls with a client of its own dispatcher: cancelling its calls on reconnect leaves the messages being sent
     * through the shared client alone.
     */
    private suspend fun poll(connection: TelegramConnection.Ready, botName: String, onRegistered: () -> Unit) {
        val statusInterceptor = GetUpdatesStatusInterceptor(
            botName = botName,
            failureMapper = failureMapper,
            onState = { state -> connectionState.value = state }
        )
        val pollingClient = connection.okHttpClient.newBuilder()
            .dispatcher(Dispatcher())
            .addInterceptor(statusInterceptor)
            .build()
        val pollerExecutor = Executors.newSingleThreadScheduledExecutor()
        val tgLpApplication = TelegramBotsLongPollingApplication(
            Supplier(::ObjectMapper),
            Supplier { pollingClient },
            Supplier { pollerExecutor },
            Supplier { CappedBackOff() }
        )
        try {
            runInterruptible {
                tgLpApplication.registerBot(
                    connection.token,
                    Supplier { connection.url },
                    DefaultGetUpdatesGenerator(),
                    consumer
                )
            }
            onRegistered.invoke()
            verbose { "#poll bot is registered" }
            awaitCancellation()
        } finally {
            verbose { "#poll closing TelegramBotsLongPollingApplication..." }
            statusInterceptor.deactivate()
            pollingClient.dispatcher.cancelAll()
            runCatching { tgLpApplication.close() }
                .onFailure { error(it) { "#poll could not close the polling: ${it.message}" } }
            pollerExecutor.shutdownNow()
            pollingClient.dispatcher.executorService.shutdown()
        }
    }

    private fun log(state: TelegramConnectionState) {
        when (state) {
            TelegramConnectionState.Disabled -> info { translation.telegram.guide.toMessengerText() }
            TelegramConnectionState.Connecting -> verbose { "#connectionState connecting to Telegram" }
            is TelegramConnectionState.Connected -> info { "Telegram bot ${state.botName} is connected" }
            is TelegramConnectionState.Failed -> error {
                failureTextMapper.map(state.failure).toMessengerText()
            }
        }
    }

    private fun toMessengerStatus(state: TelegramConnectionState): MessengerStatus = when (state) {
        TelegramConnectionState.Disabled -> MessengerStatus.Disabled
        TelegramConnectionState.Connecting -> MessengerStatus.Connecting
        is TelegramConnectionState.Connected -> MessengerStatus.Connected(state.botName)
        is TelegramConnectionState.Failed -> MessengerStatus.Failed(failureTextMapper.lazyMap(state.failure))
    }

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            bridgeBotJob.cancel()
            connectionStateLogJob.cancel()
            telegramMessageController.cancel()
        }
    )

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes

        const val RELAYED_MESSAGE_CACHE_CAPACITY = 1000
    }
}
