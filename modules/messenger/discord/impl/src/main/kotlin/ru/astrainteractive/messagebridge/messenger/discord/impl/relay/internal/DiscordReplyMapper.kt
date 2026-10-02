package ru.astrainteractive.messagebridge.messenger.discord.impl.relay.internal

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom

internal class DiscordReplyMapper {
    private val sourceTagRegex = MessageFrom.entries
        .joinToString(separator = "|", prefix = "^\\[(", postfix = ")] ") { from -> Regex.escape(from.short) }
        .toRegex()

    private fun authorName(replied: Message): String {
        if (replied.isWebhookMessage) {
            return replied.author.name.replaceFirst(sourceTagRegex, "")
        }
        return replied.member?.nickname ?: replied.author.name
    }

    fun map(message: Message): BEvent.Text.Reply? {
        if (message.type != MessageType.INLINE_REPLY) return null
        val replied = message.referencedMessage ?: return null
        return BEvent.Text.Reply(
            author = authorName(replied),
            authorId = if (replied.isWebhookMessage) null else replied.author.idLong,
            text = replied.contentRaw
        )
    }
}
