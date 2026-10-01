package ru.astrainteractive.messagebridge.onboarding.secret

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.input.refuse

/** Paper writes the commands of players into latest.log, so a player confirms a secret with `--unsafe`. */
internal class SecretGuard(translationKrate: CachedKrate<PluginTranslation>) {
    private val translation by translationKrate

    fun allow(sender: KCommandSender, input: SecretInput): Result<SecretInput> {
        if (sender is KPlayerKCommandSender && !input.isUnsafe) return refuse(translation.setup.unsafeRequired)
        return Result.success(input)
    }
}
