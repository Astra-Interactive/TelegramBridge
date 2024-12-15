package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.minecraft.server.MinecraftServer
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.MessageFrom
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.util.NativeComponentExt.toNative

internal class ForgeBEventConsumer(
    translationKrate: Krate<PluginTranslation>,
    private val getServer: () -> MinecraftServer?
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("MessageBridge-MinecraftMessageController") {
    private val translation by translationKrate

    override suspend fun consume(bEvent: BEvent) {
        if (bEvent.from == MessageFrom.MINECRAFT) return
        val component = when (bEvent) {
            is Text -> {
                translation.minecraftMessageFormat(
                    playerName = bEvent.author,
                    message = bEvent.text,
                    from = bEvent.from.short
                )
            }

            ServerOpenBEvent,
            ServerClosedBEvent,
            is PlayerLeaveBEvent,
            is PlayerJoinedBEvent,
            is PlayerDeathBEvent -> null
        }?.let(KyoriComponentSerializer.Legacy::toComponent) ?: return

        getServer.invoke()?.playerList?.players.orEmpty().forEach { player ->
            player.sendSystemMessage(component.toNative())
        }
    }

    init {
        BEventChannel
            .bEvents
            .onEach { bEvent -> consume(bEvent) }
            .launchIn(this)
    }
}
