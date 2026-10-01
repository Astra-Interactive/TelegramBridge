package ru.astrainteractive.messagebridge.onboarding.impl.status.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.status.api.StatusReport

internal class StatusLiteralArgumentBuilder(
    private val reports: List<StatusReport>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("status") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    val sender = ctx.getSender()
                    sender.sendMessage(translation.setup.status.header)
                    reports.flatMap(StatusReport::lines).forEach(sender::sendMessage)
                }
            }
        }
    }
}
