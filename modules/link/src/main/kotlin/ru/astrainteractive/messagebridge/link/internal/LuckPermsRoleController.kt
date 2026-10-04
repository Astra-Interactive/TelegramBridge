package ru.astrainteractive.messagebridge.link.internal

import net.luckperms.api.LuckPerms
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import java.util.UUID

internal class LuckPermsRoleController(
    configKrate: CachedKrate<PluginConfiguration>,
    private val luckPermsProvider: LuckPermsProvider
) : Logger by JUtiltLogger("MessageBridge-LuckPermsRoleController").withoutParentHandlers() {
    private val config by configKrate

    private val luckPermsOrNull: LuckPerms?
        get() = luckPermsProvider.provide().getOrNull()

    fun addLinkRole(uuid: UUID) {
        val link = config.link ?: return
        val luckPerms = luckPermsOrNull ?: run {
            error { "LuckPerms not found!" }
            return
        }
        luckPerms.userManager.modifyUser(uuid) { user ->
            val groupNode = luckPerms.nodeBuilderRegistry.forInheritance().group(link.linkLuckPermsRole).build()
            if (user.nodes.contains(groupNode)) {
                return@modifyUser
            }

            val result = user.data().add(groupNode)
            info { "Игроку $uuid выдана роль ${link.linkLuckPermsRole}: $result" }
        }.whenComplete { _, failure ->
            if (failure != null) error(failure) { "Could not grant ${link.linkLuckPermsRole} to $uuid" }
        }
    }

    fun removeLinkedRole(uuid: UUID) {
        val link = config.link ?: return
        val luckPerms = luckPermsOrNull ?: run {
            error { "LuckPerms not found!" }
            return
        }
        luckPerms.userManager.modifyUser(uuid) { user ->
            val groupNode = luckPerms.nodeBuilderRegistry.forInheritance().group(link.linkLuckPermsRole).build()
            if (!user.nodes.contains(groupNode)) {
                return@modifyUser
            }

            val result = user.data().remove(groupNode)
            info { "Игроку $uuid выдана роль ${link.linkLuckPermsRole}: $result" }
        }.whenComplete { _, failure ->
            if (failure != null) error(failure) { "Could not revoke ${link.linkLuckPermsRole} from $uuid" }
        }
    }
}
