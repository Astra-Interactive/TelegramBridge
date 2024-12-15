package ru.astrainteractive.messagebridge.messenger.bukkit.messaging

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import java.util.UUID

internal class MinecraftBEventConsumer(
    kyoriKrate: Krate<KyoriComponentSerializer>,
    translationKrate: Krate<PluginTranslation>,
    private val linkingDao: LinkingDao,
    private val dispatchers: KotlinDispatchers
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.Default(dispatchers.Main),
    Logger by JUtiltLogger("MessageBridge-MinecraftBEventConsumer") {
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

    init {
        BEventChannel.bEvents
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(this)
    }
}
