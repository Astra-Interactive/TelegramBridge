package ru.astrainteractive.messagebridge.commands.reload

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.config.YamlConfigFile

/** `/mb reload`. A file with an error is not applied, which the sender is told instead of a success. */
internal class ReloadLiteralArgumentBuilder(
    private val plugin: Lifecycle,
    private val files: List<YamlConfigFile<*>>,
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
                    val errors = files.mapNotNull { configFile ->
                        configFile.lastError?.let { error ->
                            translation.setup.reloadFileError(configFile.file.name, error)
                        }
                    }
                    if (errors.isEmpty()) {
                        sender.sendMessage(translation.reload.completed)
                    } else {
                        errors.forEach(sender::sendMessage)
                    }
                }
            }
        }
    }
}
