package ru.astrainteractive.messagebridge.bot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.model.Message
import ru.astrainteractive.messagebridge.utils.getValue

class TelegramBot(
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val minecraftMessageController: MinecraftMessageController,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : TelegramLongPollingBot() {
    private val config by configKrate
    private val translation by translationKrate

    override fun getBotToken(): String = config.token

    override fun getBotUsername(): String = "Empire Bridge Bot"

    fun Update.name(): String? {
        message.senderChat?.let {
            return it.userName ?: "${it.firstName ?: "Анонимус"} ${it.lastName ?: ""}"
        }
        message.from?.let {
            return it.userName ?: "${it.firstName ?: "Анонимус"} ${it.lastName ?: ""}"
        }
        return null
    }

    override fun onUpdateReceived(update: Update) {
        update ?: return
        if (config.channelID != update?.message?.chatId?.toString()) return
        if (config.topicID != update?.message?.replyToMessage?.messageId?.toString()) return
        val author = update?.name() ?: return
        val text = update.message.text ?: return
        scope.launch(dispatchers.IO) {
            onCommand(update)
            val formattedMinecraft = translation.minecraftMessageFormat(author, text).raw
            val minecraftMessage = Message.Text(
                textInternal = formattedMinecraft,
                from = Message.MessageFrom.TELEGRAM
            )
            minecraftMessageController.send(minecraftMessage)
        }
    }

    private fun onCommand(update: Update) {
        val text = update.message.text ?: return
        when (text) {
            "/chatid" -> {
                SendMessage().apply {
                    chatId = update.message.chatId.toString()
                    this.text = "chatID is $chatId"
                    update.message?.replyToMessage?.messageId?.let {
                        replyToMessageId = it
                    }
                }.also(::execute)
            }

            "/topicmessage" -> {
                SendMessage().apply {
                    chatId = update.message.chatId.toString()
                    this.text = "Topic original message id is ${update.message?.replyToMessage?.messageId}"
                    update.message?.replyToMessage?.messageId?.let {
                        replyToMessageId = it
                    }
                }.also(::execute)
            }
        }
    }
}
