package ru.astrainteractive.messagebridge.messenger.bukkit.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.server.util.asKAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.tryConsume
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import java.util.UUID

internal class MinecraftBEventConsumer(
    translationKrate: CachedKrate<PluginTranslation>,
    private val linkingDao: LinkingDao,
    private val dispatchers: KotlinDispatchers,
    private val bEventChannel: BEventChannel,
    private val scope: CoroutineScope,
) : BEventConsumer,
    Logger by JUtiltLogger("MessageBridge-MinecraftBEventConsumer") {
    private val translation by translationKrate

    private suspend fun replyPlayerName(text: BEvent.Text, reply: BEvent.Text.Reply): String {
        val authorId = reply.authorId ?: return reply.author
        val linkedPlayerModel = when (text) {
            is BEvent.Text.Discord -> linkingDao.findByDiscordId(authorId).getOrNull()
            is BEvent.Text.Telegram -> linkingDao.findByTelegramId(authorId).getOrNull()
            is BEvent.Text.Minecraft -> null
        }
        return linkedPlayerModel?.lastMinecraftName ?: reply.author
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.MINECRAFT) return

        val text = when (bEvent) {
            is BEvent.Text -> {
                val linkedPlayerModel = when (bEvent) {
                    is BEvent.Text.Discord -> {
                        linkingDao.findByDiscordId(bEvent.authorId).getOrNull()
                    }

                    is BEvent.Text.Minecraft -> {
                        linkingDao.findByUuid(UUID.fromString(bEvent.uuid)).getOrNull()
                    }

                    is BEvent.Text.Telegram -> {
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

            BEvent.ServerOpen,
            BEvent.ServerClosed,
            is BEvent.PlayerLeave,
            is BEvent.PlayerJoined,
            is BEvent.PlayerDeath -> null
        } ?: return

        withContext(dispatchers.Main) {
            Bukkit.getServer().forEachAudience { audience -> audience.asKAudience().sendMessage(text) }
        }
    }

    fun start() {
        bEventChannel
            .bEvents(scope)
            .onEach { bEvent -> tryConsume(bEvent) }
            .launchIn(scope)
    }
}
