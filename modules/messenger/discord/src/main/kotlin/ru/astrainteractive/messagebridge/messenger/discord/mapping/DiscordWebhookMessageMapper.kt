package ru.astrainteractive.messagebridge.messenger.discord.mapping

import club.minnced.discord.webhook.send.WebhookMessage
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class DiscordWebhookMessageMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

    fun map(event: Text, member: Member?): WebhookMessage = WebhookMessageBuilder()
        .setUsername(username(event, member))
        .setAvatarUrl(avatarUrl(event, member))
        .setContent(event.text.replace("@", ""))
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
        const val MC_HEADS_AVATAR_URL = "https://mc-heads.net/avatar/"
        const val TELEGRAM_AVATAR_URL =
            "https://upload.wikimedia.org/wikipedia/commons/5/5c/Telegram_Messenger.png"
    }
}
