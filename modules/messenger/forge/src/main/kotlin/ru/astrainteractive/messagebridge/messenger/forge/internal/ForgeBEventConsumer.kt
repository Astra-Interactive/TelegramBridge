package ru.astrainteractive.messagebridge.messenger.forge.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.astrainteractive.astralibs.server.sendMessage
import ru.astrainteractive.astralibs.server.util.MinecraftUtil
import ru.astrainteractive.astralibs.server.util.asKAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom

internal class ForgeBEventConsumer(
    translationKrate: CachedKrate<PluginTranslation>,
    private val bEventChannel: BEventChannel,
    private val scope: CoroutineScope,
) : BEventConsumer,
    Logger by JUtiltLogger("MessageBridge-ForgeBEventConsumer") {
    private val translation by translationKrate

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.MINECRAFT) return
        val text = when (bEvent) {
            is BEvent.Text -> {
                val reply = bEvent.reply
                if (reply == null) {
                    translation.chat.toMinecraft(
                        playerName = bEvent.author,
                        message = bEvent.text,
                        from = bEvent.from.short
                    )
                } else {
                    translation.chat.toMinecraftReply(
                        playerName = bEvent.author,
                        message = bEvent.text,
                        from = bEvent.from.short,
                        replyPlayerName = reply.author,
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

        MinecraftUtil.serverOrNull?.playerList?.players.orEmpty()
            .map { player -> player.asKAudience() }
            .sendMessage(text)
    }

    fun start() {
        bEventChannel
            .bEvents(scope)
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(scope)
    }
}
