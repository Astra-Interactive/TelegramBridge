package ru.astrainteractive.messagebridge.messenger.discord.messaging

import net.dv8tion.jda.api.entities.Message
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.discord.util.RestActionExt.await

internal class DiscordMessageSender :
    Logger by JUtiltLogger("MessageBridge-DiscordMessageSender").withoutParentHandlers() {

    suspend fun reply(message: Message, text: String) {
        runCatching { message.reply(text).await() }
            .propagateCancellationException()
            .onFailure { t -> error(t) { "#reply could not reply to message ${message.id}" } }
    }
}
