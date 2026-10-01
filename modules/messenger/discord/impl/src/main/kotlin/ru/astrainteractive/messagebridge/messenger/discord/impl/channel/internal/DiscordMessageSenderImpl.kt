package ru.astrainteractive.messagebridge.messenger.discord.impl.channel.internal

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.entities.Message
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.api.util.withRetry
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.api.util.await

internal class DiscordMessageSenderImpl :
    DiscordMessageSender,
    Logger by JUtiltLogger("MessageBridge-DiscordMessageSender") {

    override suspend fun reply(message: Message, text: String) {
        flow { emit(message.reply(text).await()) }
            .withRetry(this)
            .catch { failure -> error(failure) { "#reply could not reply to message ${message.id}" } }
            .collect()
    }
}
