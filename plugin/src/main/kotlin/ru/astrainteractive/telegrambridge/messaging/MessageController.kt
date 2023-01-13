package ru.astrainteractive.telegrambridge.messaging

import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.util.DiscordUtil
import org.bukkit.Bukkit
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.astrainteractive.astralibs.di.getValue
import ru.astrainteractive.telegrambridge.modules.Modules


class MessageController() : IMessageController {
    private val translation by Modules.TranslationModule
    private val config by Modules.PluginConfigModule
    private val bot by Modules.bridgeBotModule

    override suspend fun sendToMinecraft(message: Message) {
        when(message){
            is Message.Text -> Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(message.text)
            }
        }
    }

    override suspend fun sendToDiscord(message: Message) {
        kotlin.runCatching {
            when(message){
                is Message.Text -> DiscordSRV.getPlugin().mainTextChannel.sendMessage(message.text).queue()
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    override suspend fun sendToTelegram(message: Message) {
        val sendMessage = SendMessage().apply {
            this.chatId = config.channelID
            this.replyToMessageId = config.topicID.toIntOrNull()
        }
        when (message) {
            is Message.Text -> sendMessage.text = message.text
        }
        bot.execute(sendMessage)
    }


}