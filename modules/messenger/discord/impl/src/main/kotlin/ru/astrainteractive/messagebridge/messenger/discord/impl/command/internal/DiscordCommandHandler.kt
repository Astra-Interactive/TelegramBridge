package ru.astrainteractive.messagebridge.messenger.discord.impl.command.internal

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.api.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.impl.command.model.DiscordCommand

internal class DiscordCommandHandler(
    private val messageSender: DiscordMessageSender,
    private val onlinePlayersProvider: OnlinePlayersProvider,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordCommandHandler") {
    private val translation by translationKrate

    private suspend fun sendVanilla(event: MessageReceivedEvent) {
        verbose { "#sendVanilla !vanilla executed" }
        val players = onlinePlayersProvider.provide()
        val text = translation.onlinePlayers.message(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).toMessengerText()
        messageSender.reply(event.message, text)
    }

    suspend fun handle(command: DiscordCommand, event: MessageReceivedEvent) {
        when (command) {
            DiscordCommand.Vanilla -> sendVanilla(event)
        }
    }
}
