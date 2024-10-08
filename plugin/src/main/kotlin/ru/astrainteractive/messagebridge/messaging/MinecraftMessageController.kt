package ru.astrainteractive.messagebridge.messaging

import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.model.Message
import ru.astrainteractive.messagebridge.utils.getValue

class MinecraftMessageController(
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>
) : MessageController, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    override suspend fun send(message: Message) {
        val component = when (message) {
            is Message.Text -> {
                translation.minecraftMessageFormat(
                    playerName = message.author,
                    message = message.text
                )
            }

            is Message.PlayerLeave,
            is Message.PlayerJoined,
            is Message.PlayerDeath -> null
        }?.let(kyori::toComponent) ?: return
        Bukkit.broadcast(component)
    }
}
