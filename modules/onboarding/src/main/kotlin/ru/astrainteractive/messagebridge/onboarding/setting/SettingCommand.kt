package ru.astrainteractive.messagebridge.onboarding.setting

import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.input.InvalidInputError
import ru.astrainteractive.messagebridge.onboarding.messenger.Messenger

/**
 * Where a /mb command that changes config.yml answers: a refused value is replied at once, a valid one is saved off
 * the server thread, since saving writes the file and may wait for the bot.
 */
internal class SettingCommand(
    private val saver: SettingSaver,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    /** Any failure other than [InvalidInputError] is a bug, which the exception handler logs. */
    private fun replyRefused(ctx: CommandContext<Any>, sender: KCommandSender, error: Throwable) {
        if (error is InvalidInputError) {
            sender.sendMessage(error.reply)
        } else {
            commandExceptionHandler.handle(ctx, error)
        }
    }

    private fun <C> launch(
        ctx: CommandContext<Any>,
        setting: Result<Setting<C>>,
        block: suspend (KCommandSender, Setting<C>) -> Unit
    ) {
        val sender = with(multiplatformCommand) { ctx.getSender() }
        setting
            .onSuccess { validSetting ->
                ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) {
                    block.invoke(sender, validSetting)
                }
            }
            .onFailure { error -> replyRefused(ctx, sender, error) }
    }

    fun <C> save(ctx: CommandContext<Any>, messenger: Messenger<C>, setting: Result<Setting<C>>) {
        launch(ctx, setting) { sender, validSetting -> saver.save(sender, messenger, validSetting) }
    }

    fun <C> saveAndConnect(ctx: CommandContext<Any>, messenger: Messenger<C>, setting: Result<Setting<C>>) {
        launch(ctx, setting) { sender, validSetting -> saver.saveAndConnect(sender, messenger, validSetting) }
    }
}
