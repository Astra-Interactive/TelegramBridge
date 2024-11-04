package ru.astrainteractive.messagebridge.messenger.bukkit.messaging

import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

internal class MinecraftMessageController(
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>
) : MessageController, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    override suspend fun send(serverEvent: ServerEvent) {
        if (serverEvent.from == ServerEvent.MessageFrom.MINECRAFT) return
        val component = when (serverEvent) {
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
        }?.let(kyori::toComponent) ?: return
        val stringText = KyoriComponentSerializer.Plain.serializer.serialize(component)
        Bukkit.broadcastMessage(stringText)
    }
}
