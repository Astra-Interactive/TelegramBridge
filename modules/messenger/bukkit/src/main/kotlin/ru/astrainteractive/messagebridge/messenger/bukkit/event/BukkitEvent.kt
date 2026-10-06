package ru.astrainteractive.messagebridge.messenger.bukkit.event

import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import ru.astrainteractive.astralibs.event.flowEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import java.util.UUID

internal class BukkitEvent(
    configKrate: CachedKrate<PluginConfiguration>,
    plugin: Plugin
) {
    private val config by configKrate

    private val playerJoinEvent: Flow<BEvent> = flowEvent<PlayerJoinEvent>(plugin, EventPriority.MONITOR)
        .buffer(Channel.UNLIMITED)
        .filterIsInstance<PlayerJoinEvent>()
        .filter { config.displayJoinMessage }
        .map { event -> event.player }
        .map { player ->
            PlayerJoinedBEvent(
                name = player.name,
                uuid = player.uniqueId.toString(),
                hasPlayedBefore = player.hasPlayedBefore()
            )
        }

    private val playerQuitEvent: Flow<BEvent> = flowEvent<PlayerQuitEvent>(plugin, EventPriority.MONITOR)
        .buffer(Channel.UNLIMITED)
        .filterIsInstance<PlayerQuitEvent>()
        .filter { config.displayLeaveMessage }
        .map { event -> event.player }
        .map { player -> PlayerLeaveBEvent(name = player.name, uuid = player.uniqueId.toString()) }

    private val asyncChatEvent: Flow<BEvent> = flowEvent<AsyncChatEvent>(plugin, EventPriority.MONITOR)
        .buffer(Channel.UNLIMITED)
        .filterIsInstance<AsyncChatEvent>()
        .filterNot { event -> event.isCancelled }
        .map { event ->
            Text.Minecraft(
                author = event.player.name,
                text = PlainTextComponentSerializer.plainText().serialize(event.message()),
                uuid = event.player.uniqueId.toString(),
                ref = MessageRef.Minecraft(messageId = UUID.randomUUID().toString())
            )
        }

    private val playerDeathEvent: Flow<BEvent> = flowEvent<PlayerDeathEvent>(plugin, EventPriority.MONITOR)
        .buffer(Channel.UNLIMITED)
        .filterIsInstance<PlayerDeathEvent>()
        .filterNot { event -> event.isCancelled }
        .filter { config.displayDeathMessage }
        .map { event ->
            PlayerDeathBEvent(
                name = event.entity.name,
                cause = event.deathMessage,
                uuid = event.entity.uniqueId.toString()
            )
        }

    val bEvents: Flow<BEvent> = merge(playerJoinEvent, playerQuitEvent, asyncChatEvent, playerDeathEvent)
}
