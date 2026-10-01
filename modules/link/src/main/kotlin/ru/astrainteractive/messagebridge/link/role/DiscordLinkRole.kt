package ru.astrainteractive.messagebridge.link.role

import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger

/** The Discord role config.yml gives for a linked Discord account. */
internal class DiscordLinkRole : Logger by JUtiltLogger("MessageBridge-DiscordLinkRole") {

    /** @param roleId id of the role as typed into config.yml, so it may be not a number */
    fun give(member: Member, roleId: String) {
        val guild = member.guild
        val role = roleId.toLongOrNull()?.let(guild::getRoleById)
        if (role == null) {
            error { "#give the role $roleId is not found on ${guild.name}" }
            return
        }
        guild.addRoleToMember(member, role).queue(
            { _ -> info { "#give ${member.id} got the role ${role.name}" } },
            { throwable -> error(throwable) { "#give could not give the role ${role.name} to ${member.id}" } }
        )
    }
}
