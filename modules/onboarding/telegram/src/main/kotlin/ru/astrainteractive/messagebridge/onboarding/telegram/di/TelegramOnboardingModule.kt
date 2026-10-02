package ru.astrainteractive.messagebridge.onboarding.telegram.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.di.TelegramBotModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.botUserName
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.di.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramBindHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramSetupCommandParser
import ru.astrainteractive.messagebridge.onboarding.telegram.event.TelegramSetupInterceptor
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
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

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
        logger = JUtiltLogger("MessageBridge-TelegramGuideLogger"),
    )

    val updateInterceptor: TelegramUpdateInterceptor = TelegramSetupInterceptor(
        scope = moduleIoScope,
        commandParser = TelegramSetupCommandParser(
            botUserName = { botModule.state.value.botUserName },
        ),
        bindHandler = TelegramBindHandler(
            bindCodes = bindCodes,
            botApi = botModule.botApi,
            messageSender = botModule.messageSender,
            configKrate = coreModule.configKrate,
            translationKrate = onboardingTranslationModule.translationKrate,
        ),
        chatInfoHandler = TelegramChatInfoHandler(
            messageSender = botModule.messageSender,
            translationKrate = onboardingTranslationModule.translationKrate,
        ),
    )

    override val onboarding: MessengerOnboarding = TelegramOnboarding(
        status = botModule.state
            .map(statusMapper::map)
            .stateIn(moduleIoScope, SharingStarted.Eagerly, MessengerStatus.Connecting),
        deliveryError = botModule.deliveryError,
        bindCodes = bindCodes,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = { botModule.state.onEach(guideLogger::log).launchIn(moduleIoScope) },
        onDisable = { moduleIoScope.cancel() }
    )

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes
    }
}
