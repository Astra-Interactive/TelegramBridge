package ru.astrainteractive.messagebridge.messenger.discord.fake

import net.dv8tion.jda.api.entities.Message
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageSender
import java.util.concurrent.CopyOnWriteArrayList

class RecordingDiscordMessageSender : DiscordMessageSender {
    val replies: MutableList<String> = CopyOnWriteArrayList()

    override suspend fun reply(message: Message, text: String) {
        replies += text
    }
}
