package ru.astrainteractive.messagebridge.link.controller

import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration

class DiscordRoleController(
    configKrate: CachedKrate<PluginConfiguration>,
) : Logger by JUtiltLogger("MessageBridge-DiscordRoleController").withoutParentHandlers() {
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
