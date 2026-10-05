package ru.astrainteractive.messagebridge.messenger.discord.api

import ru.astrainteractive.messagebridge.messenger.api.model.Text

fun interface DiscordAuthorResolver {
    suspend fun discordUserId(text: Text): Long?
}
