package ru.astrainteractive.messagebridge.link.event

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener

internal class LinkedMemberLeaveListener(
    private val linkingDao: LinkingDao,
    private val luckPermsRoleController: LuckPermsRoleController
) : DiscordMemberLeaveListener, Logger by JUtiltLogger("MessageBridge-LinkedMemberLeaveListener") {
    override suspend fun onMemberLeave(discordUserId: Long) {
        val player = linkingDao.findByDiscordId(discordUserId).getOrElse { t ->
            error(t) { "#onMemberLeave could not read the link of Discord user $discordUserId" }
            return
        } ?: return
        luckPermsRoleController.removeLinkedRole(player.uuid)
    }
}
