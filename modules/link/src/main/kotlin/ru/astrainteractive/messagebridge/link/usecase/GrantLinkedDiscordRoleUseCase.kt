package ru.astrainteractive.messagebridge.link.usecase

import kotlinx.coroutines.channels.SendChannel
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.link.util.discordRoleIdOrNull
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange

internal class GrantLinkedDiscordRoleUseCase(
    configKrate: CachedKrate<PluginConfiguration>,
    private val roleChanges: SendChannel<DiscordRoleChange>
) : Logger by JUtiltLogger("MessageBridge-GrantLinkedDiscordRoleUseCase") {
    private val config by configKrate

    suspend operator fun invoke(discordUserId: Long) {
        val link = config.link ?: return
        val roleId = link.discordRoleIdOrNull() ?: run {
            error { "#invoke linkDiscordRole ${link.linkDiscordRole} is not a Discord role id" }
            return
        }
        roleChanges.send(DiscordRoleChange.Grant(discordUserId = discordUserId, roleId = roleId))
    }
}
