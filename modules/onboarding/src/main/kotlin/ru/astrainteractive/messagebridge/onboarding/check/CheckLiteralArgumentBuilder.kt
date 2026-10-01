package ru.astrainteractive.messagebridge.onboarding.check

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.messenger.Messenger

/** `/mb <messenger> check`: checks the bot off the server thread, since it asks the messenger and sends a message. */
internal class CheckLiteralArgumentBuilder(
    private val messenger: Messenger<*>,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    /** The texts of a check are plain, so the mark and the color of its level are added here. */
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
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) { check(sender) }
                }
            }
        }
    }
}
