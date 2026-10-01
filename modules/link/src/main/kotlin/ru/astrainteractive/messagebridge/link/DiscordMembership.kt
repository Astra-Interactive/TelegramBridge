package ru.astrainteractive.messagebridge.link

/**
 * Keeps links in step with the members of the server of the bridge channel: a linked account that left is
 * unlinked and loses the LuckPerms group, unless a linked Telegram account still gives it. Nothing changes while
 * config.yml gives no roles for linking.
 */
interface DiscordMembership {
    suspend fun revokeLeftMember(discordId: Long)

    /**
     * Revokes every linked account that is not among [memberIds], for the members who left while the bot was
     * offline. An empty [memberIds] changes nothing: the bot is a member itself, so it can only be a failed load.
     */
    suspend fun revokeAbsentMembers(memberIds: Set<Long>)
}
