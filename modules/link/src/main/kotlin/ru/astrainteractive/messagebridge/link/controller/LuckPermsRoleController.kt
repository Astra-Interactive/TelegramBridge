package ru.astrainteractive.messagebridge.link.controller

import net.luckperms.api.LuckPerms
import net.luckperms.api.node.types.InheritanceNode
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.controller.di.factory.LuckPermsFactory
import java.util.UUID

class LuckPermsRoleController(
    configKrate: Krate<PluginConfiguration>,
    private val luckPermsFactory: LuckPermsFactory
) : Logger by JUtiltLogger("MessageBridge-LuckPermsRoleController") {
    private val config by configKrate

    private val luckPermsOrNull: LuckPerms?
        get() = luckPermsFactory.provide()

    fun addLinkRole(uuid: UUID) {
        val link = config.link ?: return
        val luckPerms = luckPermsOrNull ?: run {
            error { "LuckPerms not found!" }
            return
        }
        luckPerms.userManager.modifyUser(uuid) {
            val groupNode = InheritanceNode.builder(link.linkLuckPermsRole).build()
            if (it.nodes.contains(groupNode)) {
                return@modifyUser
            }

            val result = it.data().add(groupNode)
            info { "Игроку $uuid выдана роль ${link.linkLuckPermsRole}: $result" }
        }
    }

    fun removeLinkedRole(uuid: UUID) {
        val link = config.link ?: return
        val luckPerms = luckPermsOrNull ?: run {
            error { "LuckPerms not found!" }
            return
        }
        luckPerms.userManager.modifyUser(uuid) {
            val groupNode = InheritanceNode.builder(link.linkLuckPermsRole).build()
            if (!it.nodes.contains(groupNode)) {
                return@modifyUser
            }

            val result = it.data().remove(groupNode)
            info { "Игроку $uuid выдана роль ${link.linkLuckPermsRole}: $result" }
        }
    }
}
