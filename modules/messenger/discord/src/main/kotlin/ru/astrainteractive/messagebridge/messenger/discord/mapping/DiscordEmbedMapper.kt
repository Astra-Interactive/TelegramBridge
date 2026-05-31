package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent

@Suppress("MagicNumber")
internal class DiscordEmbedMapper {

    fun map(event: PlayerDeathBEvent): MessageEmbed = EmbedBuilder()
        .setColor(DEATH_COLOR)
        .setAuthor(event.cause ?: "${event.name} сдох =))", null, avatarUrl(event.uuid))
        .build()

    fun map(event: PlayerJoinedBEvent): MessageEmbed {
        val text = if (event.hasPlayedBefore) {
            "${event.name} присоединился"
        } else {
            "${event.name} присоединился впервые!"
        }
        val color = if (event.hasPlayedBefore) JOIN_COLOR else FIRST_JOIN_COLOR
        return EmbedBuilder()
            .setColor(color)
            .setAuthor(text, null, avatarUrl(event.uuid))
            .build()
    }

    fun map(event: PlayerLeaveBEvent): MessageEmbed = EmbedBuilder()
        .setColor(LEAVE_COLOR)
        .setAuthor("${event.name} покинул нас", null, avatarUrl(event.uuid))
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
