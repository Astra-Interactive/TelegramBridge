package ru.astrainteractive.messagebridge.messaging

import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

class MinecraftMessageController(
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>
) : MessageController, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    override suspend fun send(messageEvent: MessageEvent) {
        if (messageEvent.from == MessageEvent.MessageFrom.MINECRAFT) return
        val component = when (messageEvent) {
            is MessageEvent.Text -> {
                translation.minecraftMessageFormat(
                    playerName = messageEvent.author,
                    message = messageEvent.text,
                    from = messageEvent.from.short
                )
            }

            MessageEvent.ServerOpen,
            MessageEvent.ServerClosed,
            is MessageEvent.PlayerLeave,
            is MessageEvent.PlayerJoined,
            is MessageEvent.PlayerDeath -> null
        }?.let(kyori::toComponent) ?: return
        Bukkit.broadcast(component)
    }
}
