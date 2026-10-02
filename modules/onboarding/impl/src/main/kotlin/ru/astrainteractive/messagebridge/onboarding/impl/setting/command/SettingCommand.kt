package ru.astrainteractive.messagebridge.onboarding.impl.setting.command

import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.impl.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.impl.model.InvalidInputError
import ru.astrainteractive.messagebridge.onboarding.impl.setting.internal.SettingSaver
import ru.astrainteractive.messagebridge.onboarding.impl.setting.model.Setting

internal class SettingCommand(
    private val saver: SettingSaver,
    private val ioScope: CoroutineScope,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    private fun replyRefused(ctx: CommandContext<Any>, sender: KCommandSender, t: Throwable) {
        if (t is InvalidInputError) {
            sender.sendMessage(t.reply)
        } else {
            commandExceptionHandler.handle(ctx, t)
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
            .onFailure { t -> replyRefused(ctx, sender, t) }
    }

    fun <C> save(ctx: CommandContext<Any>, messenger: Messenger<C>, setting: Result<Setting<C>>) {
        launch(ctx, setting) { sender, validSetting -> saver.save(sender, messenger, validSetting) }
    }

    fun <C> saveAndConnect(ctx: CommandContext<Any>, messenger: Messenger<C>, setting: Result<Setting<C>>) {
        launch(ctx, setting) { sender, validSetting -> saver.saveAndConnect(sender, messenger, validSetting) }
    }
}
