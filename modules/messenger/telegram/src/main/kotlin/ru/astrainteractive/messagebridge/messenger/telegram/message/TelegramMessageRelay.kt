package ru.astrainteractive.messagebridge.messenger.telegram.message

import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.api.api.intercept
import ru.astrainteractive.messagebridge.messenger.api.model.Interception
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.message.mapping.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.message.mapping.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.message.model.TelegramMessageValidation

internal class TelegramMessageRelay(
    private val commandParser: TelegramCommandMapper,
    private val commandHandler: TelegramCommandHandler,
    private val replyMapper: TelegramReplyMapper,
    private val messageSender: TelegramMessageSender,
    private val bEventConsumer: BEventConsumer,
    private val messageInterceptors: List<MessageInterceptor<Update>>,
) {
    private suspend fun answer(update: Update, text: String) {
        val message = update.message ?: return
        messageSender.send(
            message.chatId.toString(),
            text,
            message.replyToMessage?.messageId
        )
    }

    suspend fun relay(update: Update, valid: TelegramMessageValidation.Valid) {
        val command = commandParser.toCommand(valid.text)
        if (command != null) {
            commandHandler.replyToCommand(command, update)
            return
        }
        val interception = messageInterceptors.intercept(update)
        if (interception is Interception.Reply) {
            answer(update, interception.text)
        }
        if (interception != Interception.Pass) return
        val message = update.message ?: return
        bEventConsumer.consume(
            Text.Telegram(
                author = valid.author,
                text = valid.text,
                authorId = valid.authorId,
                reply = replyMapper.toReply(message),
                ref = MessageRef.Telegram(chatId = message.chatId, messageId = message.messageId),
            )
        )
    }
}
