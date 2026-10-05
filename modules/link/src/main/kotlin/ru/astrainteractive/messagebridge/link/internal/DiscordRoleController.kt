package ru.astrainteractive.messagebridge.link.internal

import kotlinx.coroutines.channels.SendChannel
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange

internal class DiscordRoleController(
    configKrate: CachedKrate<PluginConfiguration>,
    private val roleChanges: SendChannel<DiscordRoleChange>
) : Logger by JUtiltLogger("MessageBridge-DiscordRoleController") {
    private val config by configKrate

    suspend fun addLinkedRole(discordUserId: Long) {
        val link = config.link ?: return
        val roleId = link.linkDiscordRole.toLongOrNull() ?: run {
            error { "#addLinkedRole linkDiscordRole ${link.linkDiscordRole} is not a Discord role id" }
            return
        }
        roleChanges.send(DiscordRoleChange.Grant(discordUserId = discordUserId, roleId = roleId))
    }

    suspend fun removeLinkedRole(discordUserId: Long) {
        val link = config.link ?: return
        val roleId = link.linkDiscordRole.toLongOrNull() ?: run {
            error { "#removeLinkedRole linkDiscordRole ${link.linkDiscordRole} is not a Discord role id" }
            return
        }
        roleChanges.send(DiscordRoleChange.Revoke(discordUserId = discordUserId, roleId = roleId))
    }
}
