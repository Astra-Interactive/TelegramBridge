package ru.astrainteractive.messagebridge.onboarding.telegram.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramSetupCommandParser
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramBindHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.model.TelegramSetupCommand

internal class TelegramSetupInterceptor(
    private val scope: CoroutineScope,
    private val commandParser: TelegramSetupCommandParser,
    private val bindHandler: TelegramBindHandler,
    private val chatInfoHandler: TelegramChatInfoHandler,
) : TelegramUpdateInterceptor {
    private suspend fun handle(command: TelegramSetupCommand, message: Message) {
        when (command) {
            is TelegramSetupCommand.Bind -> bindHandler.bind(command.code, message)
            TelegramSetupCommand.ChatInfo -> chatInfoHandler.sendChatInfo(message)
        }
    }

    override fun intercept(update: Update): Boolean {
        val message = update.message ?: return false
        val command = message.text?.let(commandParser::map) ?: return false
        scope.launch { handle(command, message) }
        return true
    }
}
