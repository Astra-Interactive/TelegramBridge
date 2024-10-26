package ru.astrainteractive.messagebridge.link.controller

import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue

class DiscordRoleController(
    configKrate: Krate<PluginConfiguration>,
) : Logger by JUtiltLogger("MessageBridge-DiscordRoleController") {
    private val config by configKrate

    fun addLinkedRole(member: Member) {
        val link = config.link ?: return
        val guild = member.guild
        val role = guild.getRoleById(link.linkDiscordRole) ?: run {
            error { "#accountLinked could not find role with id ${link.linkDiscordRole}" }
            return
        }
        guild.addRoleToMember(member, role).queue()
    }

    fun removeLinkedRole(member: Member) {
        val link = config.link ?: return
        val guild = member.guild
        val role = guild.getRoleById(link.linkDiscordRole) ?: run {
            error { "#accountLinked could not find role with id ${link.linkDiscordRole}" }
            return
        }
        guild.removeRoleFromMember(member, role).queue()
    }
}
