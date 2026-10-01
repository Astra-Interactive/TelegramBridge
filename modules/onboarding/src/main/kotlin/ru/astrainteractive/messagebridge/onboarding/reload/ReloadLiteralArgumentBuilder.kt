package ru.astrainteractive.messagebridge.onboarding.reload

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.config.describeConfigError

/**
 * `/mb reload`. A config.yml with an error is not applied and the bots keep their settings, which the sender is
 * told instead of a success.
 *
 * @param configKrate config.yml as the reload of [plugin] read it
 */
internal class ReloadLiteralArgumentBuilder(
    private val plugin: Lifecycle,
    private val configKrate: CachedKrate<Result<PluginConfiguration>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

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
                            sender.sendMessage(translation.setup.reloadConfigError(describeConfigError(error)))
                        }
                }
            }
        }
    }
}
