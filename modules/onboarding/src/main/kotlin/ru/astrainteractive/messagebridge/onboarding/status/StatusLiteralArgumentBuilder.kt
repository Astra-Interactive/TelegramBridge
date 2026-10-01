package ru.astrainteractive.messagebridge.onboarding.status

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler

/** `/mb status`: the [reports] of every messenger under one header. */
internal class StatusLiteralArgumentBuilder(
    private val reports: List<StatusReport>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("status") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    sender.sendMessage(translation.setup.status.header)
                    reports.flatMap(StatusReport::lines).forEach(sender::sendMessage)
                }
            }
        }
    }
}
