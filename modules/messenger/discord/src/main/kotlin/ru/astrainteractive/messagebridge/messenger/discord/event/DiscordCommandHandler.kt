package ru.astrainteractive.messagebridge.messenger.discord.event

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.api.LinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordCommand

internal class DiscordCommandHandler(
    private val messageSender: DiscordMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    private val linkApi: LinkApi,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordCommandHandler").withoutParentHandlers() {
    private val translation by translationKrate

    suspend fun handle(command: DiscordCommand, event: MessageReceivedEvent) {
        when (command) {
            DiscordCommand.Vanilla -> sendVanilla(event)
            is DiscordCommand.Link -> sendLink(command.code, event)
        }
    }

    /**
     * Handles a direct message where the whole content is the linking code.
     */
    suspend fun linkFromPrivate(event: MessageReceivedEvent) {
        val member = event.member ?: return
        val code = event.message.contentRaw.toIntOrNull() ?: INVALID_CODE
        link(code, member, event)
    }

    private suspend fun sendVanilla(event: MessageReceivedEvent) {
        info { "#sendVanilla !vanilla executed" }
        val players = onlinePlayersProvider.provide()
        val text = translation.onlinePlayersMessage(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).raw
        messageSender.reply(event.message, text)
    }

    private suspend fun sendLink(code: Int, event: MessageReceivedEvent) {
        val member = event.member ?: return
        link(code, member, event)
    }

    private suspend fun link(code: Int, member: Member, event: MessageReceivedEvent) {
        val response = linkApi.linkDiscord(code, member)
        val text = response.asMessage(translation.link).raw
        messageSender.reply(event.message, text)
    }

    private companion object {
        const val INVALID_CODE = -1
    }
}
