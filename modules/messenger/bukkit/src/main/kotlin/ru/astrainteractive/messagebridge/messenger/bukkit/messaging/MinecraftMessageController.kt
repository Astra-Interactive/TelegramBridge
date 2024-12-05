package ru.astrainteractive.messagebridge.messenger.bukkit.messaging

import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import java.util.UUID

internal class MinecraftMessageController(
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>,
    private val linkingDao: LinkingDao,
) : MessageController, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    override suspend fun send(serverEvent: ServerEvent) {
        if (serverEvent.from == ServerEvent.MessageFrom.MINECRAFT) return

        val component = when (serverEvent) {
            is ServerEvent.Text -> {
                val linkedPlayerModel = when (serverEvent) {
                    is ServerEvent.Text.Discord -> {
                        linkingDao.findByDiscordId(serverEvent.authorId).getOrNull()
                    }

                    is ServerEvent.Text.Minecraft -> {
                        linkingDao.findByUuid(UUID.fromString(serverEvent.uuid)).getOrNull()
                    }

                    is ServerEvent.Text.Telegram -> {
                        linkingDao.findByTelegramId(serverEvent.authorId).getOrNull()
                    }
                }

                translation.minecraftMessageFormat(
                    playerName = linkedPlayerModel?.lastMinecraftName ?: serverEvent.author,
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
