package ru.astrainteractive.messagebridge.messenger.bukkit.internal

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.server.util.asKAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.tryConsume
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import java.util.UUID

internal class MinecraftBEventConsumer(
    translationKrate: CachedKrate<PluginTranslation>,
    private val linkingDao: LinkingDao,
    private val dispatchers: KotlinDispatchers,
    private val bEventChannel: BEventChannel
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-MinecraftBEventConsumer") {
    private val translation by translationKrate

    private suspend fun replyPlayerName(text: Text, reply: Text.Reply): String {
        val authorId = reply.authorId ?: return reply.author
        val linkedPlayerModel = when (text) {
            is Text.Discord -> linkingDao.findByDiscordId(authorId).getOrNull()
            is Text.Telegram -> linkingDao.findByTelegramId(authorId).getOrNull()
            is Text.Minecraft -> null
        }
        return linkedPlayerModel?.lastMinecraftName ?: reply.author
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.MINECRAFT) return

        val text = when (bEvent) {
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

                val reply = bEvent.reply
                if (reply == null) {
                    translation.chat.toMinecraft(
                        playerName = linkedPlayerModel?.lastMinecraftName ?: bEvent.author,
                        message = bEvent.text,
                        from = bEvent.from.short
                    )
                } else {
                    translation.chat.toMinecraftReply(
                        playerName = linkedPlayerModel?.lastMinecraftName ?: bEvent.author,
                        message = bEvent.text,
                        from = bEvent.from.short,
                        replyPlayerName = replyPlayerName(bEvent, reply),
                        replyMessage = reply.text
                    )
                }
            }

            ServerOpenBEvent,
            ServerClosedBEvent,
            is PlayerLeaveBEvent,
            is PlayerJoinedBEvent,
            is PlayerDeathBEvent -> null
        } ?: return

        withContext(dispatchers.Main) {
            Bukkit.getServer().forEachAudience { audience -> audience.asKAudience().sendMessage(text) }
        }
    }

    init {
        bEventChannel
            .bEvents(this)
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(this)
    }
}
