package ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.impl.api.BindInstruction

internal class TelegramBindInstruction(
    private val messenger: TelegramMessenger,
    translationKrate: CachedKrate<OnboardingTranslation>
) : BindInstruction {
    private val translation by translationKrate

    private fun messageOf(code: String): String {
        val botName = messenger.onboarding.status.value.tryCast<MessengerStatus.Connected>()
            ?.botName
            ?.removePrefix("@")
        return if (botName.isNullOrBlank()) "$BIND_COMMAND $code" else "$BIND_COMMAND@$botName $code"
    }

    override fun of(code: BindCode): LocalizableComponent {
        return translation.setup.telegramBindIssued(messageOf(code.value), code.lifetime)
    }

    private companion object {
        const val BIND_COMMAND = "/bind"
    }
}
