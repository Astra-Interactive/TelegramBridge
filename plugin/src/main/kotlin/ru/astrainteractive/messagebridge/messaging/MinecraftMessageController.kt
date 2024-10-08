package ru.astrainteractive.messagebridge.messaging

import org.bukkit.Bukkit
import ru.astrainteractive.messagebridge.messaging.model.Message

class MinecraftMessageController : MessageController {

    override suspend fun send(message: Message) {
        when (message) {
            is Message.Text -> Bukkit.broadcastMessage(message.formattedText)
        }
    }
}
