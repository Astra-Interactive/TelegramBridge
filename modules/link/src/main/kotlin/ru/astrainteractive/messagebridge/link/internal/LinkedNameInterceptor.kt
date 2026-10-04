package ru.astrainteractive.messagebridge.link.internal

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayer
import ru.astrainteractive.messagebridge.messaging.api.TextInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Text

internal class LinkedNameInterceptor(
    private val linkingDao: LinkingDao
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
                author = linkedName(text.authorId, linkingDao::findByDiscordId) ?: text.author,
                reply = linkedReply(text.reply, linkingDao::findByDiscordId)
            )

            is Text.Telegram -> text.copy(
                author = linkedName(text.authorId, linkingDao::findByTelegramId) ?: text.author,
                reply = linkedReply(text.reply, linkingDao::findByTelegramId)
            )

            is Text.Minecraft -> text
        }
    }
}
