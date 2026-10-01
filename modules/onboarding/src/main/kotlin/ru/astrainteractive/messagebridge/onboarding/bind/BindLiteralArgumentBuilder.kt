package ru.astrainteractive.messagebridge.onboarding.bind

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.messenger.Messenger
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.status.StatusText

/**
 * `/mb <messenger> bind`: a one-time code that binds the chat it is sent to. The sender is told once the chat is
 * bound; a bot without a token can not read the code, so the sender is told how to set the token instead.
 */
internal class BindLiteralArgumentBuilder(
    private val messenger: Messenger<*>,
    private val instruction: BindInstruction,
    private val statusText: StatusText,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    private fun issueCode(sender: KAudience) {
        val status = messenger.onboarding.status.value
        if (status == MessengerStatus.Disabled) {
            sender.sendMessage(statusText.stateOf(messenger, status))
            return
        }
        val code = messenger.onboarding.issueBindCode { bound -> sender.sendMessage(bound) }
        sender.sendMessage(instruction.of(code))
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("bind") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    issueCode(ctx.getSender())
                }
            }
        }
    }
}
