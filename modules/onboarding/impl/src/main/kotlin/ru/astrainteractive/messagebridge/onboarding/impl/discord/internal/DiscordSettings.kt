package ru.astrainteractive.messagebridge.onboarding.impl.discord.internal

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.impl.secret.model.SecretInput
import ru.astrainteractive.messagebridge.onboarding.impl.util.refuse

internal class DiscordSettings(
    private val secretGuard: SecretGuard,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    fun token(sender: KCommandSender, value: String): Result<String> {
        val input = secretGuard.allow(sender, SecretInput.parse(value))
            .getOrElse { t -> return Result.failure(t) }
        val token = input.words.singleOrNull()?.takeIf(TOKEN::matches)
            ?: return refuse(translation.setup.invalidDiscordToken)
        return Result.success(token)
    }

    fun channel(value: String): Result<String> {
        if (!SNOWFLAKE.matches(value)) return refuse(translation.setup.invalidChannel)
        return Result.success(value)
    }

    private companion object {
        val TOKEN = Regex("^[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{10,}$")
        val SNOWFLAKE = Regex("^\\d{17,20}$")
    }
}
