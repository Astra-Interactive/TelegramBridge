package ru.astrainteractive.telegrambridge.bot

import github.scarsz.discordsrv.DiscordSRV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage.SendMessageBuilder
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.astralibs.async.PluginScope
import ru.astrainteractive.astralibs.di.IDependency
import ru.astrainteractive.astralibs.di.getValue
import ru.astrainteractive.telegrambridge.messaging.Message
import ru.astrainteractive.telegrambridge.modules.Modules
import ru.astrainteractive.telegrambridge.utils.PluginConfiguration

class BridgeBot(configModule: IDependency<PluginConfiguration>) : TelegramLongPollingBot() {
    private val config by configModule
    private val messageController by Modules.messageControllerModule
    private val translation by Modules.TranslationModule
    override fun getBotToken(): String = config.token

    override fun getBotUsername(): String = "Empire Bridge Bot"

    override fun onUpdateReceived(update: Update) {
        update ?: return
        if (config.channelID != update?.message?.chatId?.toString()) return
        if (config.topicID != update?.message?.replyToMessage?.messageId?.toString()) return
        val sender = update.message.senderChat?:return
        val author = sender.userName?:"${sender.firstName?:"Анонимус"} ${sender.lastName?:""}"
        PluginScope.launch(Dispatchers.IO) {
            onCommand(update)
            val formattedDiscord = translation.discordMessageFormat(author, update.message.text)
            val discordMessage = Message.Text(formattedDiscord,Message.MessageFrom.TELEGRAM)
            messageController.sendToDiscord(discordMessage)

            val formattedMinecraft = translation.minecraftMessageFormat(author, update.message.text)
            val minecraftMessage = Message.Text(formattedDiscord,Message.MessageFrom.TELEGRAM)
            messageController.sendToMinecraft(minecraftMessage)
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

