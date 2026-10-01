package ru.astrainteractive.messagebridge.onboarding.telegram.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramBotModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramSetupCommandParser
import ru.astrainteractive.messagebridge.onboarding.telegram.event.TelegramSetupInterceptor
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramBindHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramDiagnostics
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramGuideLogger
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramOnboarding
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramStatusMapper
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class TelegramOnboardingModule(
    coreModule: CoreModule,
    onboardingTranslationModule: OnboardingTranslationModule,
    botModule: TelegramBotModule,
) : MessengerOnboardingModule<MessengerOnboarding> {
    private val scope = CoroutineScope(
        coreModule.ioScope.coroutineContext + SupervisorJob(coreModule.ioScope.coroutineContext[Job])
    )

    private val bindCodes = BindCodes(
        clock = Clock.System,
        lifetime = BIND_CODE_LIFETIME,
        random = SecureRandom(),
    )

    private val statusMapper = TelegramStatusMapper(
        failureTextMapper = botModule.failureTextMapper,
    )

    private val guideLogger = TelegramGuideLogger(
        translationKrate = onboardingTranslationModule.translationKrate,
        logger = JUtiltLogger("MessageBridge-TelegramOnboarding"),
    )

    val updateInterceptor: TelegramUpdateInterceptor = TelegramSetupInterceptor(
        scope = scope,
        commandParser = TelegramSetupCommandParser(
            botUserName = { (botModule.state.value as? TelegramConnectionState.Connected)?.botName?.removePrefix("@") },
        ),
        bindHandler = TelegramBindHandler(
            bindCodes = bindCodes,
            botApi = botModule.botApi,
            messageSender = botModule.messageSender,
            configKrate = coreModule.configKrate,
            translationKrate = onboardingTranslationModule.translationKrate,
            logger = JUtiltLogger("MessageBridge-TelegramBindHandler"),
        ),
        chatInfoHandler = TelegramChatInfoHandler(
            messageSender = botModule.messageSender,
            translationKrate = onboardingTranslationModule.translationKrate,
            logger = JUtiltLogger("MessageBridge-TelegramChatInfoHandler"),
        ),
    )

    override val onboarding: MessengerOnboarding = TelegramOnboarding(
        status = botModule.state
            .map(statusMapper::map)
            .stateIn(scope, SharingStarted.Eagerly, MessengerStatus.Connecting),
        deliveryError = botModule.deliveryError,
        bindCodes = bindCodes,
        diagnostics = TelegramDiagnostics(
            configFlow = coreModule.config,
            translationKrate = onboardingTranslationModule.translationKrate,
            connectionState = botModule.state,
            botApi = botModule.botApi,
            failureTextMapper = botModule.failureTextMapper,
        ),
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = { scope.cancel() }
    )

    init {
        botModule.state.onEach(guideLogger::log).launchIn(scope)
    }

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes
    }
}
