package ru.astrainteractive.messagebridge.onboarding.impl.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.api.BindInstruction
import ru.astrainteractive.messagebridge.onboarding.impl.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.impl.status.internal.StatusText

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
                    ctx.requirePermission(OnboardingPermission.Setup)
                    issueCode(ctx.getSender())
                }
            }
        }
    }
}
