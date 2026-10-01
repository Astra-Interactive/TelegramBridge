package ru.astrainteractive.messagebridge.link.api.api

interface DiscordMembership {
    suspend fun revokeLeftMember(discordId: Long)

    suspend fun revokeAbsentMembers(memberIds: Set<Long>)
}
