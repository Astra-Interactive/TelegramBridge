package ru.astrainteractive.messagebridge.messenger.bukkit.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.astrainteractive.astralibs.event.EventListener
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

/**
 * This is a most convenient way to use bukkit events in kotlin
 */
internal class BukkitEvent(
    configKrate: Krate<PluginConfiguration>,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : EventListener, Logger by JUtiltLogger("MessageBridge-BukkitEvent") {
    private val config by configKrate

    @EventHandler(ignoreCancelled = true)
    fun playerJoin(it: PlayerJoinEvent) {
        if (!config.displayJoinMessage) return

        scope.launch(dispatchers.IO) {
            val bEvent = PlayerJoinedBEvent(
                name = it.player.name,
                uuid = it.player.uniqueId.toString(),
                hasPlayedBefore = it.player.hasPlayedBefore()
            )
            BEventChannel.consume(bEvent)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun playerLeaveEvent(it: PlayerQuitEvent) {
        if (!config.displayLeaveMessage) return
        scope.launch(dispatchers.IO) {
            val bEvent = PlayerLeaveBEvent(
                name = it.player.name,
                uuid = it.player.uniqueId.toString()
            )
            BEventChannel.consume(bEvent)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun asyncMessageEvent(it: AsyncPlayerChatEvent) {
        val message = KyoriComponentSerializer.Plain.toComponent(it.message)
        val player = it.player

        scope.launch(dispatchers.IO) {
            val textComponent = message as TextComponent
            val bEvent = Text.Minecraft(
                author = player.name,
                text = textComponent.content(),
                uuid = player.uniqueId.toString()
            )
            BEventChannel.consume(bEvent)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun deathEvent(it: PlayerDeathEvent) {
        if (!config.displayDeathMessage) return
        scope.launch(dispatchers.IO) {
            val deathCause = it.deathMessage
            val bEvent = PlayerDeathBEvent(
                name = it.entity.name,
                cause = deathCause,
                uuid = it.entity.uniqueId.toString()
            )
            BEventChannel.consume(bEvent)
        }
    }
}
