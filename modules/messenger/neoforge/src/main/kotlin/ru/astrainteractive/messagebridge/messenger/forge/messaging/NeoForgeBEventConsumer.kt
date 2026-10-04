package ru.astrainteractive.messagebridge.messenger.forge.messaging

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.server.sendMessage
import ru.astrainteractive.astralibs.server.util.MinecraftUtil
import ru.astrainteractive.astralibs.server.util.asKAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.Text
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom

internal class NeoForgeBEventConsumer(
    translationKrate: CachedKrate<PluginTranslation>,
    private val dispatchers: KotlinDispatchers,
    private val bEventReceiver: BEventReceiver,
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-NeoForgeBEventConsumer").withoutParentHandlers() {
    private val translation by translationKrate

    private suspend fun show(bEvent: BEvent) {
        val text = when (bEvent) {
            is Text -> {
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

            ServerOpenBEvent,
            ServerClosedBEvent,
            is PlayerLeaveBEvent,
            is PlayerJoinedBEvent,
            is PlayerDeathBEvent -> null
        } ?: return

        withContext(dispatchers.Main) {
            MinecraftUtil.serverOrNull?.playerList?.players.orEmpty()
                .map { player -> player.asKAudience() }
                .sendMessage(text)
        }
    }

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.MINECRAFT) return
        runCatching { show(bEvent) }
            .propagateCancellationException()
            .onFailure { t -> error(t) { "#consume could not show $bEvent in game" } }
    }

    init {
        bEventReceiver
            .bEvents(this)
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(this)
    }
}
