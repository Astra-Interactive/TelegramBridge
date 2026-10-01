package ru.astrainteractive.messagebridge.onboarding.discord.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordBotModule
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageInterceptor
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindCommandParser
import ru.astrainteractive.messagebridge.onboarding.discord.event.DiscordBindInterceptor
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordBindHandler
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordDiagnostics
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordGuideLogger
import ru.astrainteractive.messagebridge.onboarding.discord.internal.JdaDiscordOnboarding
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DiscordOnboardingModule(
    coreModule: CoreModule,
    onboardingTranslationModule: OnboardingTranslationModule,
    botModule: DiscordBotModule,
) : MessengerOnboardingModule<DiscordOnboarding> {
    private val scope = CoroutineScope(
        context = coreModule.ioScope.coroutineContext + SupervisorJob(coreModule.ioScope.coroutineContext.job)
    )

    private val bindCodes = BindCodes(
        clock = Clock.System,
        lifetime = BIND_CODE_LIFETIME,
        random = SecureRandom(),
    )

    private val guideLogger = DiscordGuideLogger(
        translationKrate = onboardingTranslationModule.translationKrate,
        logger = JUtiltLogger("MessageBridge-DiscordOnboarding"),
    )

    val messageInterceptor: DiscordMessageInterceptor = DiscordBindInterceptor(
        scope = scope,
        commandParser = DiscordBindCommandParser(),
        bindHandler = DiscordBindHandler(
            bindCodes = bindCodes,
            configKrate = coreModule.configKrate,
            messageSender = botModule.messageSender,
            translationKrate = onboardingTranslationModule.translationKrate,
        ),
    )

    override val onboarding: DiscordOnboarding = JdaDiscordOnboarding(
        connection = botModule.connection,
        failureTextMapper = botModule.failureTextMapper,
        bindCodes = bindCodes,
        diagnostics = DiscordDiagnostics(
            connection = botModule.connection,
            failureTextMapper = botModule.failureTextMapper,
            configFlow = coreModule.config,
            translationKrate = onboardingTranslationModule.translationKrate,
        ),
        deliveryError = botModule.deliveryError,
        scope = scope,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = { scope.cancel() }
    )

    init {
        botModule.connection.onEach(guideLogger::log).launchIn(scope)
    }

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes
    }
}
