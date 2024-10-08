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
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.Message
import ru.astrainteractive.messagebridge.utils.getValue

/**
 * This is a most convenient way to use bukkit events in kotlin
 */
class BukkitEvent(
    configKrate: Krate<PluginConfiguration>,
    translationKrate: Krate<PluginTranslation>,
    private val telegramMessageController: MessageController,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : EventListener {
    private val config by configKrate
    private val translation by translationKrate

    @EventHandler
    fun playerJoin(it: PlayerJoinEvent) {
        if (!config.displayJoinMessage) return
        scope.launch(dispatchers.IO) {
            val message = Message.Text(
                textInternal = translation.playerJoinMessage(it.player.name).raw,
                from = Message.MessageFrom.MINECRAFT
            )
            telegramMessageController.send(message)
        }
    }

    @EventHandler
    fun playerLeaveEvent(it: PlayerQuitEvent) {
        if (!config.displayLeaveMessage) return
        scope.launch(dispatchers.IO) {
            val message = Message.Text(
                textInternal = translation.playerLeaveMessage(it.player.name).raw,
                from = Message.MessageFrom.MINECRAFT
            )
            telegramMessageController.send(message)
        }
    }

    @EventHandler
    fun messageEvent(it: AsyncChatEvent) {
        scope.launch(dispatchers.IO) {
            val textComponent = it.message() as TextComponent
            val message = Message.Text(
                textInternal = translation.telegramMessageFormat(it.player.name, textComponent.content()).raw,
                from = Message.MessageFrom.MINECRAFT
            )
            telegramMessageController.send(message)
        }
    }

    @EventHandler
    fun deathEvent(it: PlayerDeathEvent) {
        if (!config.displayDeathMessage) return
        scope.launch(dispatchers.IO) {
            val textComponent = (it.deathMessage() as? TextComponent?)?.content() ?: it.deathMessage
            val message = Message.Text(
                textInternal = translation.playerDiedMessage(it.player.name, textComponent ?: "Непанятна че").raw,
                from = Message.MessageFrom.MINECRAFT
            )
            telegramMessageController.send(message)
        }
    }
}
