package ru.astrainteractive.messagebridge.link.discord.command

import kotlinx.coroutines.flow.StateFlow
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.api.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.mapping.asMessage
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.discord.internal.DiscordLinkRole
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.util.awaitRequest
import ru.astrainteractive.messagebridge.messenger.discord.util.findTextChannel

internal class DiscordLinkHandler(
    private val linkApi: LinkApi,
    private val linkRole: DiscordLinkRole,
    private val messageSender: DiscordMessageSender,
    private val configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<LinkTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordLinkHandler") {
    private val translation by translationKrate

    private val config: PluginConfiguration
        get() = configFlow.value

    private suspend fun bridgeServerMemberOrNull(event: MessageReceivedEvent): Member? {
        val channelId = config.jdaConfig.channelId
        val channel = event.jda.findTextChannel(channelId)
        if (channel == null) {
            verbose { "#bridgeServerMemberOrNull the bridge channel $channelId is not available" }
            return null
        }
        return awaitRequest { channel.guild.retrieveMember(event.author) }
            .onFailure { t -> verbose { "#bridgeServerMemberOrNull the author is not on the bridge server: $t" } }
            .getOrNull()
    }

    private suspend fun link(code: Int, member: Member, event: MessageReceivedEvent) {
        val discordLink = LinkedPlayerModel.DiscordLink(
            lastDiscordName = member.effectiveName,
            discordId = member.idLong
        )
        val response = linkApi.linkDiscord(code, discordLink)
        if (response is LinkResponse.Linked) {
            config.link?.let { link -> linkRole.give(member, link.linkDiscordRole) }
        }
        messageSender.reply(event.message, response.asMessage(translation.link).toMessengerText())
    }

    suspend fun linkInChannel(code: Int, event: MessageReceivedEvent) {
        val member = event.member ?: return
        link(code, member, event)
    }

    suspend fun linkFromPrivate(code: Int, event: MessageReceivedEvent) {
        val member = event.member ?: bridgeServerMemberOrNull(event) ?: return
        link(code, member, event)
    }
}
