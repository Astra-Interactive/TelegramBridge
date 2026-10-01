package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation

internal class TelegramGuideLogger(
    translationKrate: CachedKrate<OnboardingTranslation>,
    logger: Logger,
) : Logger by logger {
    private val translation by translationKrate

    fun log(state: TelegramConnectionState) {
        if (state != TelegramConnectionState.Disabled) return
        info { translation.telegram.guide.toMessengerText() }
    }
}
