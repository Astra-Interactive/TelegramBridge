package ru.astrainteractive.messagebridge.link.event

import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener

internal class LinkedMemberLeaveListener(
    private val linkApi: LinkApi
) : DiscordMemberLeaveListener {
    override suspend fun onMemberLeave(discordUserId: Long) {
        linkApi.userLeaveDiscord(discordUserId)
    }
}
