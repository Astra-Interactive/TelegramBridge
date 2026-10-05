package ru.astrainteractive.messagebridge.messenger.discord.internal

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent

@Suppress("MagicNumber")
internal class DiscordEmbedMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

    fun map(event: PlayerDeathBEvent): MessageEmbed {
        val text = event.cause ?: translation.player.discordDiedOfUnknownCause(event.name).toMessengerText()
        return EmbedBuilder()
            .setColor(DEATH_COLOR)
            .setAuthor(text, null, avatarUrl(event.uuid))
            .build()
    }

    fun map(event: PlayerJoinedBEvent): MessageEmbed {
        val text = if (event.hasPlayedBefore) {
            translation.player.discordJoined(event.name).toMessengerText()
        } else {
            translation.player.discordJoinedFirstTime(event.name).toMessengerText()
        }
        val color = if (event.hasPlayedBefore) JOIN_COLOR else FIRST_JOIN_COLOR
        return EmbedBuilder()
            .setColor(color)
            .setAuthor(text, null, avatarUrl(event.uuid))
            .build()
    }

    fun map(event: PlayerLeaveBEvent): MessageEmbed = EmbedBuilder()
        .setColor(LEAVE_COLOR)
        .setAuthor(translation.player.discordLeft(event.name).toMessengerText(), null, avatarUrl(event.uuid))
        .build()

    private fun avatarUrl(uuid: String): String = "$MC_HEADS_AVATAR_URL$uuid"

    private companion object {
        const val MC_HEADS_AVATAR_URL = "https://mc-heads.net/avatar/"
        const val DEATH_COLOR = 0xb5123b
        const val LEAVE_COLOR = 0xb5123b
        const val JOIN_COLOR = 0x1fb82c
        const val FIRST_JOIN_COLOR = 0x34ebe5
    }
}
