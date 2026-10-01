package ru.astrainteractive.messagebridge.link.telegram.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.telegram.command.TelegramLinkHandler
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramBotCommand

internal class TelegramLinkInterceptor(
    private val scope: CoroutineScope,
    private val configFlow: StateFlow<PluginConfiguration>,
    private val botUserName: () -> String?,
    private val linkHandler: TelegramLinkHandler,
) : TelegramUpdateInterceptor {
    private fun codeOrNull(message: Message): Int? {
        if (message.chatId?.toString() != configFlow.value.tgConfig.chatID) return null
        val command = message.text?.let { text -> TelegramBotCommand.parse(text, botUserName()) } ?: return null
        if (command.name != LINK) return null
        return command.argument.toIntOrNull() ?: INVALID_CODE
    }

    override fun intercept(update: Update): Boolean {
        val message = update.message ?: return false
        val code = codeOrNull(message) ?: return false
        scope.launch { linkHandler.link(code, message) }
        return true
    }

    private companion object {
        const val LINK = "/link"
        const val INVALID_CODE = -1
    }
}
