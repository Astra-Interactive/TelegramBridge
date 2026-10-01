package ru.astrainteractive.messagebridge.messenger.discord.impl.command.internal

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.api.util.awaitRequest
import ru.astrainteractive.messagebridge.messenger.discord.impl.channel.internal.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.impl.command.model.DiscordCommand

internal class DiscordCommandHandler(
    private val messageSender: DiscordMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi,
    private val channelProvider: DiscordChannelProvider,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordCommandHandler") {
    private val translation by translationKrate

    private suspend fun bridgeServerMemberOrNull(event: MessageReceivedEvent): Member? {
        val channel = channelProvider.textChannel(event.jda).getOrElse { failure ->
            verbose { "#linkFromPrivate the bridge channel is not available: ${failure.message}" }
            return null
        }
        return awaitRequest { channel.guild.retrieveMember(event.author) }
            .onFailure { failure -> verbose { "#linkFromPrivate the author is not on the bridge server: $failure" } }
            .getOrNull()
    }

    private suspend fun sendVanilla(event: MessageReceivedEvent) {
        verbose { "#sendVanilla !vanilla executed" }
        val players = onlinePlayersProvider.provide()
        val text = translation.onlinePlayers.message(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).toMessengerText()
        messageSender.reply(event.message, text)
    }

    private suspend fun link(code: Int, member: Member, event: MessageReceivedEvent) {
        val response = linkApi.linkDiscord(code, member)
        val text = response.asMessage(translation.link).toMessengerText()
        messageSender.reply(event.message, text)
    }

    private suspend fun sendLink(code: Int, event: MessageReceivedEvent) {
        val member = event.member ?: return
        link(code, member, event)
    }

    suspend fun handle(command: DiscordCommand, event: MessageReceivedEvent) {
        when (command) {
            DiscordCommand.Vanilla -> sendVanilla(event)
            is DiscordCommand.Link -> sendLink(command.code, event)
        }
    }

    suspend fun linkFromPrivate(event: MessageReceivedEvent) {
        val member = event.member ?: bridgeServerMemberOrNull(event) ?: return
        val code = event.message.contentRaw.toIntOrNull() ?: INVALID_CODE
        link(code, member, event)
    }

    private companion object {
        const val INVALID_CODE = -1
    }
}
