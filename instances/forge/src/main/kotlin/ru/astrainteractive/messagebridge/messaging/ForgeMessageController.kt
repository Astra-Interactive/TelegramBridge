package ru.astrainteractive.messagebridge.messaging

import net.minecraft.network.chat.ComponentUtils
import net.minecraft.server.MinecraftServer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

internal class ForgeMessageController(
    translationKrate: Krate<PluginTranslation>,
    private val getServer: () -> MinecraftServer?
) : MessageController, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val translation by translationKrate

    override suspend fun send(serverEvent: ServerEvent) {
        if (serverEvent.from == ServerEvent.MessageFrom.MINECRAFT) return
        val stringDesc = when (serverEvent) {
            is ServerEvent.Text -> {
                translation.minecraftMessageFormat(
                    playerName = serverEvent.author,
                    message = serverEvent.text,
                    from = serverEvent.from.short
                )
            }

            ServerEvent.ServerOpen,
            ServerEvent.ServerClosed,
            is ServerEvent.PlayerLeave,
            is ServerEvent.PlayerJoined,
            is ServerEvent.PlayerDeath -> null
        } ?: return
        getServer.invoke()?.playerList?.players.orEmpty().forEach { player ->
            player.sendSystemMessage(ComponentUtils.fromMessage { stringDesc.raw })
        }
    }
}
