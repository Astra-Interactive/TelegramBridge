package ru.astrainteractive.messagebridge.core.api.command

import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineExceptionHandler
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.exception.ArgumentConverterException
import ru.astrainteractive.astralibs.command.api.exception.BadArgumentException
import ru.astrainteractive.astralibs.command.api.exception.CommandException
import ru.astrainteractive.astralibs.command.api.exception.LocalizableComponentCommandException
import ru.astrainteractive.astralibs.command.api.exception.NoPermissionException
import ru.astrainteractive.astralibs.command.api.exception.NoPlayerException
import ru.astrainteractive.astralibs.command.api.exception.NoPotionEffectTypeException
import ru.astrainteractive.astralibs.command.api.exception.NotPlayerExecutorException
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation

class CommandExceptionHandler(
    private val multiplatformCommand: MultiplatformCommand,
    translationKrate: CachedKrate<PluginTranslation>
) : Logger by JUtiltLogger("MessageBridge-CommandExceptionHandler") {
    private val translation by translationKrate

    private fun commandNameOf(ctx: CommandContext<Any>): String = ctx.input.substringBefore(' ')

    private fun messageOf(t: Throwable, commandName: String): LocalizableComponent {
        return when (t) {
            is LocalizableComponentCommandException -> t.localizableComponent
            is NoPermissionException -> translation.commandError.noPermission
            is NotPlayerExecutorException -> translation.commandError.onlyPlayerCommand
            is NoPlayerException -> translation.commandError.playerNotFound
            is ArgumentConverterException,
            is BadArgumentException,
            is NoPotionEffectTypeException -> translation.commandError.invalidArgument

            is CommandException -> translation.commandError.wrongUsage
            else -> {
                error(t) { "#messageOf /$commandName failed with an unexpected exception" }
                translation.commandError.unknownError
            }
        }
    }

    fun handle(ctx: CommandContext<Any>, t: Throwable) {
        val commandName = commandNameOf(ctx)
        val sender = runCatching { with(multiplatformCommand) { ctx.getSender() } }
            .getOrElse { senderError ->
                error(t) {
                    "#handle /$commandName failed and its sender could not be resolved: ${senderError.message}"
                }
                return
            }
        sender.sendMessage(messageOf(t, commandName))
    }

    fun coroutineExceptionHandler(ctx: CommandContext<Any>): CoroutineExceptionHandler {
        val commandName = commandNameOf(ctx)
        val sender = with(multiplatformCommand) { ctx.getSender() }
        return CoroutineExceptionHandler { _, t ->
            sender.sendMessage(messageOf(t, commandName))
        }
    }
}
