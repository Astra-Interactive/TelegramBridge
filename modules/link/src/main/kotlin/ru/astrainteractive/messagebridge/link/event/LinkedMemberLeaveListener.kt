package ru.astrainteractive.messagebridge.link.event

import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener

internal class LinkedMemberLeaveListener(
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController
) : DiscordMemberLeaveListener {
    override suspend fun onMemberLeave(discordUserId: Long) {
        val player = linkingDao.findByDiscordId(discordUserId).getOrNull() ?: return
        luckPermsRoleController.removeLinkedRole(player.uuid)
    }
}
