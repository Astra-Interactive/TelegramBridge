package ru.astrainteractive.messagebridge.onboarding.discord

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.bind.BindCode
import ru.astrainteractive.messagebridge.onboarding.bind.BindInstruction

internal class DiscordBindInstruction(translationKrate: CachedKrate<PluginTranslation>) : BindInstruction {
    private val translation by translationKrate

    override fun of(code: BindCode): LocalizableComponent {
        return translation.setup.discordBindIssued(code.value, code.lifetime)
    }
}
