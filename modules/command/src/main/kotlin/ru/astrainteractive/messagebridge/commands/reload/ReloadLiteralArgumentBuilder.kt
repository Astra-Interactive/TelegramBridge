package ru.astrainteractive.messagebridge.commands.reload

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.permission.PluginPermission

internal class ReloadLiteralArgumentBuilder(
    private val plugin: Lifecycle,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    val translation by translationKrate

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("mbreload") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Reload)
                    ctx.getSender().sendMessage(translation.reload.started)
                    plugin.onReload()
                    ctx.getSender().sendMessage(translation.reload.completed)
                }
            }
        }
    }
}
