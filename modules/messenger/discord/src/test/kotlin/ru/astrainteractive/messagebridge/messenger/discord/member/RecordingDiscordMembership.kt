package ru.astrainteractive.messagebridge.messenger.discord.member

import ru.astrainteractive.messagebridge.link.DiscordMembership

internal class RecordingDiscordMembership : DiscordMembership {
    val leftDiscordIds = mutableListOf<Long>()
    val comparedMemberIds = mutableListOf<Set<Long>>()

    override suspend fun revokeLeftMember(discordId: Long) {
        leftDiscordIds += discordId
    }

    override suspend fun revokeAbsentMembers(memberIds: Set<Long>) {
        comparedMemberIds.add(memberIds)
    }
}
