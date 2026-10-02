package ru.astrainteractive.messagebridge.onboarding.impl.secret.internal

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.impl.secret.model.SecretInput
import ru.astrainteractive.messagebridge.onboarding.impl.util.refuse

internal class SecretGuard(translationKrate: CachedKrate<OnboardingTranslation>) {
    private val translation by translationKrate

    fun allow(sender: KCommandSender, input: SecretInput): Result<SecretInput> {
        if (sender is KPlayerKCommandSender && !input.isUnsafe) return refuse(translation.setup.unsafeRequired)
        return Result.success(input)
    }
}
