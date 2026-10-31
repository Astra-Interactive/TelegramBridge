package ru.astrainteractive.messagebridge.link.usecase

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.link.api.LinkRoleRepository
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository

internal class UnlinkLeftDiscordMemberUseCase(
    private val linkedPlayerRepository: LinkedPlayerRepository,
    private val linkRoleRepository: LinkRoleRepository
) : Logger by JUtiltLogger("MessageBridge-UnlinkLeftDiscordMemberUseCase") {
    suspend operator fun invoke(discordUserId: Long) {
        val player = linkedPlayerRepository.unlinkDiscord(discordUserId).getOrElse { t ->
            error(t) { "#invoke could not unlink Discord user $discordUserId" }
            return
        } ?: return
        if (player.telegram != null) return
        linkRoleRepository.removeLinkedRole(player.uuid)
    }
}
