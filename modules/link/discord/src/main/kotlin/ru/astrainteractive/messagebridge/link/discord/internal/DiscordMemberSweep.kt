package ru.astrainteractive.messagebridge.link.discord.internal

import kotlinx.coroutines.flow.StateFlow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.GatewayIntent
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.link.api.api.DiscordMembership
import ru.astrainteractive.messagebridge.messenger.discord.api.util.awaitTask
import ru.astrainteractive.messagebridge.messenger.discord.api.util.findTextChannel

internal class DiscordMemberSweep(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val discordMembership: DiscordMembership,
) : Logger by JUtiltLogger("MessageBridge-DiscordMemberSweep") {

    suspend fun sweep(jda: JDA) {
        if (GatewayIntent.GUILD_MEMBERS !in jda.gatewayIntents) return
        val channelId = configFlow.value.jdaConfig.channelId
        val channel = jda.findTextChannel(channelId)
        if (channel == null) {
            verbose { "#sweep the bridge channel $channelId is not available" }
            return
        }
        val guild = channel.guild
        val members = awaitTask { guild.loadMembers() }.getOrElse { t ->
            error(t) { "#sweep could not load the members of ${guild.name}" }
            return
        }
        verbose { "#sweep ${members.size} members are on ${guild.name}" }
        discordMembership.revokeAbsentMembers(members.mapTo(HashSet()) { member -> member.idLong })
    }
}
