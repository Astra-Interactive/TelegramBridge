package ru.astrainteractive.messagebridge.messenger.discord.message.mapping

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordRelayedMessageCache

internal class DiscordReplyMapper(
    translationKrate: CachedKrate<PluginTranslation>,
    private val relayedMessageCache: DiscordRelayedMessageCache,
) {
    private val translation by translationKrate
    private val sourceTagRegex = MessageFrom.entries
        .joinToString(separator = "|", prefix = "^\\[(", postfix = ")] ") { from -> Regex.escape(from.short) }
        .toRegex()

    private fun String.withoutReplyHeader(): String {
        val lines = lines()
        if (lines.size < 2 || !lines.first().startsWith(SUBTEXT_PREFIX)) return this
        return lines.drop(1).joinToString("\n")
    }

    private suspend fun relayedReply(replied: Message): Text.Reply {
        val relayed = relayedMessageCache.find(replied.idLong)
        if (relayed != null) {
            return Text.Reply(
                author = relayed.author,
                authorId = null,
                text = relayed.text,
                target = relayed.ref
            )
        }
        return Text.Reply(
            author = replied.author.name.replaceFirst(sourceTagRegex, ""),
            authorId = null,
            text = replied.contentRaw.withoutReplyHeader(),
            target = null
        )
    }

    private fun serverReply(replied: Message): Text.Reply = Text.Reply(
        author = translation.chat.replyServer.toMessengerText(),
        authorId = null,
        text = replied.embeds.firstOrNull()?.author?.name ?: replied.contentRaw,
        target = null
    )

    private fun authoredReply(replied: Message): Text.Reply = Text.Reply(
        author = replied.member?.nickname ?: replied.author.name,
        authorId = replied.author.idLong,
        text = replied.contentRaw,
        target = MessageRef.Discord(messageId = replied.idLong)
    )

    suspend fun map(message: Message): Text.Reply? {
        if (message.type != MessageType.INLINE_REPLY) return null
        val replied = message.referencedMessage ?: return null
        return when {
            replied.isWebhookMessage -> relayedReply(replied)
            replied.author.idLong == replied.jda.selfUser.idLong -> serverReply(replied)
            else -> authoredReply(replied)
        }
    }

    private companion object {
        const val SUBTEXT_PREFIX = "-# "
    }
}
