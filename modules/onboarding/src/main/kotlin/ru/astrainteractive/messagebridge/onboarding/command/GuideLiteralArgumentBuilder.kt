package ru.astrainteractive.messagebridge.onboarding.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.permission.PluginPermission
import ru.astrainteractive.messagebridge.onboarding.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.status.api.StatusReport

internal class GuideLiteralArgumentBuilder(
    private val messenger: Messenger<*>,
    private val guide: () -> LocalizableComponent,
    private val report: StatusReport,
    private val subcommands: List<LiteralArgumentBuilder<Any>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command(messenger.command) {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    if (messenger.onboarding.status.value !is MessengerStatus.Connected) {
                        sender.sendMessage(guide.invoke())
                    }
                    report.lines().forEach(sender::sendMessage)
                }
                subcommands.forEach { subcommand -> then(subcommand) }
            }
        }
    }
}
