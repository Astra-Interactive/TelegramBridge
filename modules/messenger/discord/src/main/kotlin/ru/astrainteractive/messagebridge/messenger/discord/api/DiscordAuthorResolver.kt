package ru.astrainteractive.messagebridge.messenger.discord.api

import ru.astrainteractive.messagebridge.messaging.model.Text

fun interface DiscordAuthorResolver {
    suspend fun discordUserId(text: Text): Long?
}
