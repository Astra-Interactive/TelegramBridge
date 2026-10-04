package ru.astrainteractive.messagebridge.messenger.discord.event

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordCommand

internal class DiscordCommandHandler(
    private val messageSender: DiscordMessageSender,
    private val platformServer: PlatformServer,
    translationKrate: CachedKrate<PluginTranslation>,
) : Logger by JUtiltLogger("MessageBridge-DiscordCommandHandler").withoutParentHandlers() {
    private val translation by translationKrate

    suspend fun handle(command: DiscordCommand, event: MessageReceivedEvent) {
        when (command) {
            DiscordCommand.Vanilla -> sendVanilla(event)
        }
    }

    private suspend fun sendVanilla(event: MessageReceivedEvent) {
        verbose { "#sendVanilla !vanilla executed" }
        val players = platformServer
            .getOnlinePlayers()
            .map(OnlineKPlayer::name)
        val text = translation.onlinePlayers.message(
            count = players.size,
            players = players.joinToString(separator = ", "),
        ).toMessengerText()
        messageSender.reply(event.message, text)
    }
}
