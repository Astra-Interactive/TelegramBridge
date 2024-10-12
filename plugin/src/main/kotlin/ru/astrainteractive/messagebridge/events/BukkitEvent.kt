package ru.astrainteractive.messagebridge.events

import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.astrainteractive.astralibs.event.EventListener
import ru.astrainteractive.discordbot.module.bridge.api.internal.PluginBridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

/**
 * This is a most convenient way to use bukkit events in kotlin
 */
class BukkitEvent(
    configKrate: Krate<PluginConfiguration>,
    private val pluginBridgeApi: PluginBridgeApi,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : EventListener {
    private val config by configKrate

    @EventHandler
    fun playerJoin(it: PlayerJoinEvent) {
        if (!config.displayJoinMessage) return
        scope.launch(dispatchers.IO) {
            val serverEvent = ServerEvent.PlayerJoined(
                name = it.player.name,
                uuid = it.player.uniqueId.toString(),
            )
            pluginBridgeApi.broadcastEvent(ServerEventMessageData(serverEvent))
        }
    }

    @EventHandler
    fun playerLeaveEvent(it: PlayerQuitEvent) {
        if (!config.displayLeaveMessage) return
        scope.launch(dispatchers.IO) {
            val serverEvent = ServerEvent.PlayerLeave(
                name = it.player.name,
                uuid = it.player.uniqueId.toString()
            )
            pluginBridgeApi.broadcastEvent(ServerEventMessageData(serverEvent))
        }
    }

    @EventHandler
    fun messageEvent(it: AsyncChatEvent) {
        scope.launch(dispatchers.IO) {
            val textComponent = it.message() as TextComponent
            val serverEvent = ServerEvent.Text.Minecraft(
                author = it.player.name,
                text = textComponent.content(),
                uuid = it.player.uniqueId.toString()
            )
            pluginBridgeApi.broadcastEvent(ServerEventMessageData(serverEvent))
        }
    }

    @EventHandler
    fun deathEvent(it: PlayerDeathEvent) {
        if (!config.displayDeathMessage) return
        scope.launch(dispatchers.IO) {
            val deathCause = (it.deathMessage() as? TextComponent?)?.content() ?: it.deathMessage
            val serverEvent = ServerEvent.PlayerDeath(
                name = it.player.name,
                cause = deathCause,
                uuid = it.player.uniqueId.toString()
            )
            pluginBridgeApi.broadcastEvent(ServerEventMessageData(serverEvent))
        }
    }
}
