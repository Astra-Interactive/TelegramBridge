package ru.astrainteractive.messagebridge.link.event

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.role.luckperms.LuckPermsRoleController
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener

internal class LinkedMemberLeaveListener(
    private val linkedPlayerRepository: LinkedPlayerRepository,
    private val luckPermsRoleController: LuckPermsRoleController
) : DiscordMemberLeaveListener, Logger by JUtiltLogger("MessageBridge-LinkedMemberLeaveListener") {
    override suspend fun onMemberLeave(discordUserId: Long) {
        val player = linkedPlayerRepository.unlinkDiscord(discordUserId).getOrElse { t ->
            error(t) { "#onMemberLeave could not unlink Discord user $discordUserId" }
            return
        } ?: return
        if (player.telegram != null) return
        luckPermsRoleController.removeLinkedRole(player.uuid)
    }
}
