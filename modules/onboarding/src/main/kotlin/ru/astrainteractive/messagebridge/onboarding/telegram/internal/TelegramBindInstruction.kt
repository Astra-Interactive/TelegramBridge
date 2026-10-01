package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.api.BindInstruction
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

internal class TelegramBindInstruction(
    private val messenger: TelegramMessenger,
    translationKrate: CachedKrate<PluginTranslation>
) : BindInstruction {
    private val translation by translationKrate

    private fun messageOf(code: String): String {
        val botName = (messenger.onboarding.status.value as? MessengerStatus.Connected)
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
