package ru.astrainteractive.messagebridge.messenger.discord.api

fun interface DiscordMemberLeaveListener {
    suspend fun onMemberLeave(discordUserId: Long)
}
