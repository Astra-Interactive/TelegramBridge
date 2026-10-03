package ru.astrainteractive.messagebridge.onboarding.discord.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.di.DiscordBotModule
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.di.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindCommandParser
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordBindHandler
import ru.astrainteractive.messagebridge.onboarding.discord.event.DiscordBindInterceptor
import ru.astrainteractive.messagebridge.onboarding.discord.internal.JdaDiscordOnboarding
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DiscordOnboardingModule(
    coreModule: CoreModule,
    onboardingTranslationModule: OnboardingTranslationModule,
    botModule: DiscordBotModule,
) : MessengerOnboardingModule<DiscordOnboarding> {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val bindCodes = BindCodes(
        clock = Clock.System,
        lifetime = BIND_CODE_LIFETIME,
        random = SecureRandom(),
    )

    val messageInterceptor: DiscordMessageInterceptor = DiscordBindInterceptor(
        scope = moduleIoScope,
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
        deliveryError = botModule.deliveryError,
        scope = moduleIoScope,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = { moduleIoScope.cancel() }
    )

    private companion object {
        val BIND_CODE_LIFETIME = 10.minutes
    }
}
