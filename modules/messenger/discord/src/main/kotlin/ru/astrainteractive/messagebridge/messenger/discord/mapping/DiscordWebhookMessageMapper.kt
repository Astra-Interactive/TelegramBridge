package ru.astrainteractive.messagebridge.messenger.discord.mapping

import club.minnced.discord.webhook.send.WebhookMessage
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.messagebridge.messaging.model.Text

internal class DiscordWebhookMessageMapper {

    fun map(event: Text, member: Member?): WebhookMessage = WebhookMessageBuilder()
        .setUsername(username(event, member))
        .setAvatarUrl(avatarUrl(event, member))
        .setContent(event.text.replace("@", ""))
        .build()

    private fun username(event: Text, member: Member?): String = when (member) {
        null -> "[${event.from.short}] ${event.author}"
        else -> "[${event.from.short}] ${member.effectiveName}"
    }

    private fun avatarUrl(event: Text, member: Member?): String = when (event) {
        is Text.Discord -> error("Can't send discord to discord")
        is Text.Minecraft -> member?.effectiveAvatarUrl ?: "$MC_HEADS_AVATAR_URL${event.uuid}"
        is Text.Telegram -> member?.effectiveAvatarUrl ?: TELEGRAM_AVATAR_URL
    }

    private companion object {
        const val MC_HEADS_AVATAR_URL = "https://mc-heads.net/avatar/"
        const val TELEGRAM_AVATAR_URL =
            "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
    }
}
