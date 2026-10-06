package ru.astrainteractive.messagebridge.messenger.discord.mapping

import club.minnced.discord.webhook.send.WebhookMessage
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.core.api.util.ellipsize
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class DiscordWebhookMessageMapper(
    configKrate: CachedKrate<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val config by configKrate
    private val translation by translationKrate

    private fun String.escapeMarkdown(): String = replace(MARKDOWN_REGEX) { match -> "\\" + match.value }

    private fun String.toSingleLine(): String = lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotEmpty() }
        .joinToString(" ")

    private fun content(event: Text, replyJumpUrl: String?): String {
        val reply = event.reply ?: return event.text.replace("@", "")
        val replyPlayerName = reply.author.escapeMarkdown()
        val shownReplyPlayerName = replyJumpUrl?.let { url -> "[$replyPlayerName]($url)" } ?: replyPlayerName
        val replyMessage = reply.text
            .toSingleLine()
            .ellipsize(config.jdaConfig.replyPreviewLength)
            .escapeMarkdown()
        val quote = translation.chat.replyQuote(shownReplyPlayerName, replyMessage).toMessengerText()
        return translation.chat
            .toDiscordReply(message = event.text, quote = quote)
            .toMessengerText()
            .replace("@", "")
    }

    fun map(event: Text, member: Member?, replyJumpUrl: String?): WebhookMessage = WebhookMessageBuilder()
        .setUsername(username(event, member))
        .setAvatarUrl(avatarUrl(event, member))
        .setContent(content(event, replyJumpUrl))
        .build()

    private fun username(event: Text, member: Member?): String = translation.chat
        .toDiscordUsername(playerName = member?.effectiveName ?: event.author, from = event.from.short)
        .toMessengerText()

    private fun avatarUrl(event: Text, member: Member?): String = when (event) {
        is Text.Discord -> error("Can't send discord to discord")
        is Text.Minecraft -> member?.effectiveAvatarUrl ?: "$MC_HEADS_AVATAR_URL${event.uuid}"
        is Text.Telegram -> member?.effectiveAvatarUrl ?: TELEGRAM_AVATAR_URL
    }

    private companion object {
        val MARKDOWN_REGEX = Regex("""[\\*_~`|\[\]]""")
        const val MC_HEADS_AVATAR_URL = "https://mc-heads.net/avatar/"
        const val TELEGRAM_AVATAR_URL =
            "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
    }
}
