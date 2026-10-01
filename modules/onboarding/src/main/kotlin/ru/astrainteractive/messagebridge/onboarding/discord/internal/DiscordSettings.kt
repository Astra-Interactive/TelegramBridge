package ru.astrainteractive.messagebridge.onboarding.discord.internal

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration.JdaConfig
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.model.refuse
import ru.astrainteractive.messagebridge.onboarding.secret.internal.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.secret.internal.maskToken
import ru.astrainteractive.messagebridge.onboarding.secret.model.SecretInput
import ru.astrainteractive.messagebridge.onboarding.setting.model.Setting

internal class DiscordSettings(
    private val secretGuard: SecretGuard,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    fun token(sender: KCommandSender, value: String): Result<Setting<JdaConfig>> {
        val input = secretGuard.allow(sender, SecretInput.parse(value))
            .getOrElse { error -> return Result.failure(error) }
        val token = input.words.singleOrNull()?.takeIf(TOKEN::matches)
            ?: return refuse(translation.setup.invalidDiscordToken)
        val setting = Setting<JdaConfig>(saved = translation.setup.saved.token(maskToken(token))) { jdaConfig ->
            jdaConfig.copy(token = token)
        }
        return Result.success(setting)
    }

    fun channel(value: String): Result<Setting<JdaConfig>> {
        if (!SNOWFLAKE.matches(value)) return refuse(translation.setup.invalidChannel)
        val setting = Setting<JdaConfig>(saved = translation.setup.saved.channel(value)) { jdaConfig ->
            jdaConfig.copy(channelId = value)
        }
        return Result.success(setting)
    }

    fun activity(value: String): Result<Setting<JdaConfig>> {
        val activity = value.trim()
        val setting = Setting<JdaConfig>(saved = translation.setup.saved.activity(activity)) { jdaConfig ->
            jdaConfig.copy(activity = activity)
        }
        return Result.success(setting)
    }

    private companion object {
        val TOKEN = Regex("^[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{10,}$")
        val SNOWFLAKE = Regex("^\\d{17,20}$")
    }
}
