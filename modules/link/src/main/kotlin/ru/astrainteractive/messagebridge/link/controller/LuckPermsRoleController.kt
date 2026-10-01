package ru.astrainteractive.messagebridge.link.controller

import kotlinx.coroutines.flow.StateFlow
import net.luckperms.api.LuckPerms
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider
import java.util.UUID

class LuckPermsRoleController(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val luckPermsProvider: LuckPermsProvider
) : Logger by JUtiltLogger("MessageBridge-LuckPermsRoleController") {
    private val config: PluginConfiguration
        get() = configFlow.value

    private val luckPermsOrNull: LuckPerms?
        get() = luckPermsProvider.provide()

    fun addLinkRole(uuid: UUID) {
        val link = config.link ?: return
        val luckPerms = luckPermsOrNull ?: run {
            error { "LuckPerms not found!" }
            return
        }
        luckPerms.userManager.modifyUser(uuid) {
            val groupNode = luckPerms.nodeBuilderRegistry.forInheritance().group(link.linkLuckPermsRole).build()
            if (it.nodes.contains(groupNode)) {
                return@modifyUser
            }

            val result = it.data().add(groupNode)
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
        luckPerms.userManager.modifyUser(uuid) {
            val groupNode = luckPerms.nodeBuilderRegistry.forInheritance().group(link.linkLuckPermsRole).build()
            if (!it.nodes.contains(groupNode)) {
                return@modifyUser
            }

            val result = it.data().remove(groupNode)
            info { "Игроку $uuid выдана роль ${link.linkLuckPermsRole}: $result" }
        }.whenComplete { _, failure ->
            if (failure != null) error(failure) { "Could not revoke ${link.linkLuckPermsRole} from $uuid" }
        }
    }
}
