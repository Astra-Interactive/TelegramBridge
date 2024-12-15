package ru.astrainteractive.messagebridge.messenger.bukkit.messaging

import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import java.util.UUID
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

internal class MinecraftBEventConsumer(
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>,
    private val linkingDao: LinkingDao,
) : BEventConsumer, Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val kyori by kyoriKrate
    private val translation by translationKrate

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.MINECRAFT) return

        val component = when (bEvent) {
            is Text -> {
                val linkedPlayerModel = when (bEvent) {
                    is Text.Discord -> {
                        linkingDao.findByDiscordId(bEvent.authorId).getOrNull()
                    }

                    is Text.Minecraft -> {
                        linkingDao.findByUuid(UUID.fromString(bEvent.uuid)).getOrNull()
                    }

                    is Text.Telegram -> {
                        linkingDao.findByTelegramId(bEvent.authorId).getOrNull()
                    }
                }

                translation.minecraftMessageFormat(
                    playerName = linkedPlayerModel?.lastMinecraftName ?: bEvent.author,
                    message = bEvent.text,
                    from = bEvent.from.short
                )
            }

            ServerOpenBEvent,
            ServerClosedBEvent,
            is PlayerLeaveBEvent,
            is PlayerJoinedBEvent,
            is PlayerDeathBEvent -> null
        }?.let(kyori::toComponent) ?: return
        val stringText = KyoriComponentSerializer.Plain.serializer.serialize(component)
        Bukkit.broadcastMessage(stringText)
    }
}
