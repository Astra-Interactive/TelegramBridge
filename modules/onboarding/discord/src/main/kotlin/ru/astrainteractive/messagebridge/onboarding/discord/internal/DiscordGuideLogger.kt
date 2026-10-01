package ru.astrainteractive.messagebridge.onboarding.discord.internal

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation

internal class DiscordGuideLogger(
    translationKrate: CachedKrate<OnboardingTranslation>,
    logger: Logger,
) : Logger by logger {
    private val translation by translationKrate

    fun log(connection: DiscordConnection) {
        if (connection != DiscordConnection.Disabled) return
        info { translation.discord.guide.toMessengerText() }
    }
}
