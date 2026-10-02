package ru.astrainteractive.messagebridge.onboarding.impl.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.api.Messenger

internal class CheckLiteralArgumentBuilder(
    private val messenger: Messenger,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    private fun lineOf(check: Check): LocalizableComponent {
        return when (check.level) {
            CheckLevel.OK -> translation.setup.checkOk(check.message)
            CheckLevel.WARNING -> translation.setup.checkWarning(check.message)
            CheckLevel.ERROR -> translation.setup.checkError(check.message)
        }
    }

    private suspend fun check(sender: KAudience) {
        sender.sendMessage(translation.setup.checkStarted(messenger.name))
        messenger.onboarding.check().forEach { check -> sender.sendMessage(lineOf(check)) }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("check") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    val sender = ctx.getSender()
                    ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) { check(sender) }
                }
            }
        }
    }
}
