package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.Text

internal class DiscordReplyMapper {
    /** The source tag [DiscordWebhookMessageMapper] puts in front of the name of every message the bridge relays. */
    private val sourceTagRegex = MessageFrom.entries
        .joinToString(separator = "|", prefix = "^\\[(", postfix = ")] ") { from -> Regex.escape(from.short) }
        .toRegex()

    private fun authorName(replied: Message): String {
        if (replied.isWebhookMessage) {
            return replied.author.name.replaceFirst(sourceTagRegex, "")
        }
        return replied.member?.nickname ?: replied.author.name
    }

    /** @return null for a forwarded message, or when Discord did not send the replied one, e.g. it was deleted */
    fun map(message: Message): Text.Reply? {
        if (message.type != MessageType.INLINE_REPLY) return null
        val replied = message.referencedMessage ?: return null
        return Text.Reply(
            author = authorName(replied),
            authorId = if (replied.isWebhookMessage) null else replied.author.idLong,
            text = replied.contentRaw
        )
    }
}
