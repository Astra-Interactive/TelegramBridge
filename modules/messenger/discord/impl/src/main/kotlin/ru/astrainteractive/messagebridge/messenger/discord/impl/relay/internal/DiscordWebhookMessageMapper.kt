package ru.astrainteractive.messagebridge.messenger.discord.impl.relay.internal

import club.minnced.discord.webhook.send.WebhookMessage
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

internal class DiscordWebhookMessageMapper {
    private fun username(event: BEvent.Text, member: Member?): String = when (member) {
        null -> "[${event.from.short}] ${event.author}"
        else -> "[${event.from.short}] ${member.effectiveName}"
    }

    private fun avatarUrl(event: BEvent.Text, member: Member?): String = when (event) {
        is BEvent.Text.Discord -> error("Can't send discord to discord")
        is BEvent.Text.Minecraft -> member?.effectiveAvatarUrl ?: "$MC_HEADS_AVATAR_URL${event.uuid}"
        is BEvent.Text.Telegram -> member?.effectiveAvatarUrl ?: TELEGRAM_AVATAR_URL
    }

    fun map(event: BEvent.Text, member: Member?): WebhookMessage = WebhookMessageBuilder()
        .setUsername(username(event, member))
        .setAvatarUrl(avatarUrl(event, member))
        .setContent(event.text.replace("@", ""))
        .build()

    private companion object {
        const val MC_HEADS_AVATAR_URL = "https://mc-heads.net/avatar/"
        const val TELEGRAM_AVATAR_URL =
            "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
    }
}
