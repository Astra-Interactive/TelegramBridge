package ru.astrainteractive.messagebridge.messenger.discord.relay.internal

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

internal class DiscordEmbedMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

    private fun avatarUrl(uuid: String): String = "$MC_HEADS_AVATAR_URL$uuid"

    private fun embed(color: Int, text: String, uuid: String): MessageEmbed = EmbedBuilder()
        .setColor(color)
        .setAuthor(text, null, avatarUrl(uuid))
        .build()

    fun map(event: BEvent.PlayerDeath): MessageEmbed {
        val text = event.cause ?: translation.discord.chat.playerDied(event.name).toMessengerText()
        return embed(DEATH_COLOR, text, event.uuid)
    }

    fun map(event: BEvent.PlayerJoined): MessageEmbed {
        val chat = translation.discord.chat
        return if (event.hasPlayedBefore) {
            embed(JOIN_COLOR, chat.playerJoined(event.name).toMessengerText(), event.uuid)
        } else {
            embed(FIRST_JOIN_COLOR, chat.playerJoinedFirstTime(event.name).toMessengerText(), event.uuid)
        }
    }

    fun map(event: BEvent.PlayerLeave): MessageEmbed {
        return embed(LEAVE_COLOR, translation.discord.chat.playerLeft(event.name).toMessengerText(), event.uuid)
    }

    private companion object {
        const val MC_HEADS_AVATAR_URL = "https://mc-heads.net/avatar/"
        const val DEATH_COLOR = 0xb5123b
        const val LEAVE_COLOR = 0xb5123b
        const val JOIN_COLOR = 0x1fb82c
        const val FIRST_JOIN_COLOR = 0x34ebe5
    }
}
