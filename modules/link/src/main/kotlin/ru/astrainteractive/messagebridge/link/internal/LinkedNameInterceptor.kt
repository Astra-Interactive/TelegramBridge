package ru.astrainteractive.messagebridge.link.internal

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.model.LinkedPlayer
import ru.astrainteractive.messagebridge.messenger.api.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class LinkedNameInterceptor(
    private val linkedPlayerRepository: LinkedPlayerRepository
) : TextInterceptor, Logger by JUtiltLogger("MessageBridge-LinkedNameInterceptor") {
    private suspend fun linkedName(
        authorId: Long,
        findLinkedPlayer: suspend (Long) -> Result<LinkedPlayer?>
    ): String? {
        return findLinkedPlayer.invoke(authorId)
            .onFailure { t -> error(t) { "#linkedName could not read the link of $authorId" } }
            .getOrNull()
            ?.minecraftName
    }

    private suspend fun linkedReply(
        reply: Text.Reply?,
        findLinkedPlayer: suspend (Long) -> Result<LinkedPlayer?>
    ): Text.Reply? {
        val authorId = reply?.authorId ?: return reply
        val author = linkedName(authorId, findLinkedPlayer) ?: return reply
        return reply.copy(author = author)
    }

    override suspend fun intercept(text: Text): Text {
        return when (text) {
            is Text.Discord -> text.copy(
                author = linkedName(text.authorId, linkedPlayerRepository::findByDiscordId) ?: text.author,
                reply = linkedReply(text.reply, linkedPlayerRepository::findByDiscordId)
            )

            is Text.Telegram -> text.copy(
                author = linkedName(text.authorId, linkedPlayerRepository::findByTelegramId) ?: text.author,
                reply = linkedReply(text.reply, linkedPlayerRepository::findByTelegramId)
            )

            is Text.Minecraft -> text
        }
    }
}
