package ru.astrainteractive.messagebridge.onboarding.impl.discord.internal

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.impl.api.BindInstruction

internal class DiscordBindInstruction(translationKrate: CachedKrate<OnboardingTranslation>) : BindInstruction {
    private val translation by translationKrate

    override fun of(code: BindCode): LocalizableComponent {
        return translation.setup.discordBindIssued(code.value, code.lifetime)
    }
}
