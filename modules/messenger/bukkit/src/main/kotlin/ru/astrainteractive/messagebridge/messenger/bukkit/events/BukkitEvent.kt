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
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.util.ServerExt

/**
 * This is a most convenient way to use bukkit events in kotlin
 */
internal class BukkitEvent(
    configKrate: Krate<PluginConfiguration>,
    private val telegramMessageController: MessageController,
    private val discordMessageController: MessageController,
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
                hasPlayedBefore = it.player.hasPlayedBefore()
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
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
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }

    @EventHandler
    fun syncMessageEvent(it: AsyncPlayerChatEvent) {
        if (ServerExt.isPaper()) return
        val message = KyoriComponentSerializer.Plain.toComponent(it.message)
        val player = it.player

        scope.launch(dispatchers.IO) {
            val textComponent = message as TextComponent
            val serverEvent = ServerEvent.Text.Minecraft(
                author = player.name,
                text = textComponent.content(),
                uuid = player.uniqueId.toString()
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }

    @EventHandler
    fun deathEvent(it: PlayerDeathEvent) {
        if (!config.displayDeathMessage) return
        scope.launch(dispatchers.IO) {
            val deathCause = it.deathMessage
            val serverEvent = ServerEvent.PlayerDeath(
                name = it.entity.name,
                cause = deathCause,
                uuid = it.entity.uniqueId.toString()
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }
}
