package ru.astrainteractive.messagebridge.core.command

import com.mojang.brigadier.context.CommandContext
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.exception.CommandException
import ru.astrainteractive.astralibs.command.api.exception.LocalizableComponentCommandException
import ru.astrainteractive.astralibs.command.api.exception.NoPermissionException
import ru.astrainteractive.astralibs.command.api.exception.NotPlayerExecutorException
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation

/**
 * Tells the sender why their command failed. [MultiplatformCommand.runs] swallows every exception of a command,
 * so a command without this handler fails silently.
 */
class CommandExceptionHandler(
    private val multiplatformCommand: MultiplatformCommand,
    translationKrate: CachedKrate<PluginTranslation>
) : Logger by JUtiltLogger("MessageBridge-CommandExceptionHandler") {
    private val translation by translationKrate

    fun handle(ctx: CommandContext<Any>, throwable: Throwable) {
        val message: LocalizableComponent = when (throwable) {
            is LocalizableComponentCommandException -> throwable.localizableComponent
            is NoPermissionException -> translation.commandError.noPermission
            is NotPlayerExecutorException -> translation.commandError.onlyPlayerCommand
            is CommandException -> translation.commandError.wrongUsage
            else -> {
                error(throwable) { "#handle command failed with an unexpected exception" }
                translation.commandError.unknownError
            }
        }
        with(multiplatformCommand) {
            ctx.getSender().sendMessage(message)
        }
    }
}
