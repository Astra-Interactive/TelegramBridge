package ru.astrainteractive.messagebridge.messenger.bukkit.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.astrainteractive.astralibs.event.EventListener
import ru.astrainteractive.astralibs.localization.markup.KyoriComponentSerializer
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

/**
 * This is a most convenient way to use bukkit events in kotlin
 */
internal class BukkitEvent(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    private val bEventChannel: BEventChannel
) : EventListener, Logger by JUtiltLogger("MessageBridge-BukkitEvent") {
    private val config: PluginConfiguration
        get() = configFlow.value

    @EventHandler(ignoreCancelled = true)
    fun playerJoin(event: PlayerJoinEvent) {
        if (!config.displayJoinMessage) return

        ioScope.launch(dispatchers.IO) {
            val bEvent = BEvent.PlayerJoined(
                name = event.player.name,
                uuid = event.player.uniqueId.toString(),
                hasPlayedBefore = event.player.hasPlayedBefore()
            )
            bEventChannel.consume(bEvent)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun playerLeaveEvent(event: PlayerQuitEvent) {
        if (!config.displayLeaveMessage) return
        ioScope.launch(dispatchers.IO) {
            val bEvent = BEvent.PlayerLeave(
                name = event.player.name,
                uuid = event.player.uniqueId.toString()
            )
            bEventChannel.consume(bEvent)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun asyncMessageEvent(event: AsyncPlayerChatEvent) {
        val message = KyoriComponentSerializer.Plain.toComponent(event.message)
        val player = event.player

        ioScope.launch(dispatchers.IO) {
            val textComponent = message as TextComponent
            val bEvent = BEvent.Text.Minecraft(
                author = player.name,
                text = textComponent.content(),
                uuid = player.uniqueId.toString()
            )
            bEventChannel.consume(bEvent)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun deathEvent(event: PlayerDeathEvent) {
        if (!config.displayDeathMessage) return
        ioScope.launch(dispatchers.IO) {
            val deathCause = event.deathMessage
            val bEvent = BEvent.PlayerDeath(
                name = event.entity.name,
                cause = deathCause,
                uuid = event.entity.uniqueId.toString()
            )
            bEventChannel.consume(bEvent)
        }
    }
}
