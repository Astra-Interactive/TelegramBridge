package ru.astrainteractive.messagebridge.onboarding.impl.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.permission.PluginPermission
import ru.astrainteractive.messagebridge.core.api.util.describe
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation

internal class ReloadLiteralArgumentBuilder(
    private val plugin: Lifecycle,
    private val configKrate: CachedKrate<Result<PluginConfiguration>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>,
    onboardingTranslationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate
    private val onboardingTranslation by onboardingTranslationKrate

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("reload") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Reload)
                    val sender = ctx.getSender()
                    sender.sendMessage(translation.reload.started)
                    plugin.onReload()
                    configKrate.cachedValue
                        .onSuccess { _ -> sender.sendMessage(translation.reload.completed) }
                        .onFailure { error ->
                            sender.sendMessage(onboardingTranslation.setup.reloadConfigError(error.describe()))
                        }
                }
            }
        }
    }
}
