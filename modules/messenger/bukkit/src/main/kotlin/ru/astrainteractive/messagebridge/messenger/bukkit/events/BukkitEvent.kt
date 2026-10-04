package ru.astrainteractive.messagebridge.messenger.bukkit.events

import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

internal class BukkitEvent(
    configKrate: CachedKrate<PluginConfiguration>,
    private val eventFlow: (Class<out Event>) -> Flow<Event>
) {
    private val config by configKrate

    private val playerJoinEvent: Flow<BEvent> = uncancelledEvents<PlayerJoinEvent>()
        .filter { config.displayJoinMessage }
        .map { event -> event.player }
        .map { player ->
            PlayerJoinedBEvent(
                name = player.name,
                uuid = player.uniqueId.toString(),
                hasPlayedBefore = player.hasPlayedBefore()
            )
        }

    private val playerQuitEvent: Flow<BEvent> = uncancelledEvents<PlayerQuitEvent>()
        .filter { config.displayLeaveMessage }
        .map { event -> event.player }
        .map { player -> PlayerLeaveBEvent(name = player.name, uuid = player.uniqueId.toString()) }

    private val asyncChatEvent: Flow<BEvent> = uncancelledEvents<AsyncChatEvent>()
        .map { event ->
            Text.Minecraft(
                author = event.player.name,
                text = PlainTextComponentSerializer.plainText().serialize(event.message()),
                uuid = event.player.uniqueId.toString()
            )
        }

    private val playerDeathEvent: Flow<BEvent> = uncancelledEvents<PlayerDeathEvent>()
        .filter { config.displayDeathMessage }
        .map { event ->
            PlayerDeathBEvent(
                name = event.entity.name,
                cause = event.deathMessage,
                uuid = event.entity.uniqueId.toString()
            )
        }

    val bEvents: Flow<BEvent> = merge(playerJoinEvent, playerQuitEvent, asyncChatEvent, playerDeathEvent)

    private inline fun <reified T : Event> uncancelledEvents(): Flow<T> = eventFlow.invoke(T::class.java)
        .filterIsInstance<T>()
        .filterNot { event -> event is Cancellable && event.isCancelled }
}
