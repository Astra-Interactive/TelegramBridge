package ru.astrainteractive.messagebridge.onboarding.secret.internal

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.model.refuse
import ru.astrainteractive.messagebridge.onboarding.secret.model.SecretInput

internal class SecretGuard(translationKrate: CachedKrate<PluginTranslation>) {
    private val translation by translationKrate

    fun allow(sender: KCommandSender, input: SecretInput): Result<SecretInput> {
        if (sender is KPlayerKCommandSender && !input.isUnsafe) return refuse(translation.setup.unsafeRequired)
        return Result.success(input)
    }
}
