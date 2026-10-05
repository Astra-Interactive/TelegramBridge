package ru.astrainteractive.messagebridge.messenger.bukkit.internal

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import ru.astrainteractive.astralibs.coroutines.withTimings
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.util.asKAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.coroutines.CoroutineFeature
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.BEventReceiver
import ru.astrainteractive.messagebridge.messenger.api.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageFrom
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class MinecraftBEventConsumer(
    translationKrate: CachedKrate<PluginTranslation>,
    private val textInterceptors: List<TextInterceptor>,
    private val dispatchers: KotlinDispatchers,
    private val bEventReceiver: BEventReceiver
) : BEventConsumer,
    CoroutineFeature by CoroutineFeature.IO.withTimings(),
    Logger by JUtiltLogger("MessageBridge-MinecraftBEventConsumer") {
    private val translation by translationKrate

    internal suspend fun toMinecraftComponent(text: Text): LocalizableComponent {
        val shown = textInterceptors.fold(text) { current, interceptor -> interceptor.intercept(current) }
        val reply = shown.reply
        if (reply == null) {
            return translation.chat.toMinecraft(
                playerName = shown.author,
                message = shown.text,
                from = shown.from.short
            )
        }
        return translation.chat.toMinecraftReply(
            playerName = shown.author,
            message = shown.text,
            from = shown.from.short,
            replyPlayerName = reply.author,
            replyMessage = reply.text
        )
    }

    private suspend fun show(bEvent: BEvent) {
        val text = when (bEvent) {
            is Text -> toMinecraftComponent(bEvent)

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
