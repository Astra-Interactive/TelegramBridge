package ru.astrainteractive.messagebridge.link.internal

import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.messaging.api.TextInterceptor
import ru.astrainteractive.messagebridge.messaging.model.Text

internal class LinkedNameInterceptor(
    private val linkingDao: LinkingDao
) : TextInterceptor {
    private suspend fun linkedName(
        authorId: Long,
        findLinkedPlayer: suspend (Long) -> Result<LinkedPlayerModel>
    ): String? {
        return findLinkedPlayer.invoke(authorId)
            .getOrNull()
            ?.lastMinecraftName
    }

    private suspend fun linkedReply(
        reply: Text.Reply?,
        findLinkedPlayer: suspend (Long) -> Result<LinkedPlayerModel>
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
