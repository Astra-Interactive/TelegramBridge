package ru.astrainteractive.messagebridge.messenger.api.discord.api

fun interface DiscordMemberLeaveListener {
    suspend fun onMemberLeave(discordUserId: Long)
}
