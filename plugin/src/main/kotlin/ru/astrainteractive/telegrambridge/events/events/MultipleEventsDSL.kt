package ru.astrainteractive.telegrambridge.events.events

import io.papermc.paper.event.player.ChatEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent
import ru.astrainteractive.astralibs.async.PluginScope
import ru.astrainteractive.astralibs.di.getValue
import ru.astrainteractive.astralibs.events.DSLEvent
import ru.astrainteractive.telegrambridge.messaging.Message
import ru.astrainteractive.telegrambridge.modules.Modules

/**
 * This is a most convenient way to use bukkit events in kotlin
 */
class MultipleEventsDSL {
    private val config by Modules.PluginConfigModule
    private val messageController by Modules.messageControllerModule
    private val translation by Modules.TranslationModule

    val playerJoin = DSLEvent.event<PlayerJoinEvent> {
        if (!config.displayJoinMessage) return@event
        PluginScope.launch(Dispatchers.IO) {
            val message = Message.Text(translation.playerJoinMessage(it.player.name),Message.MessageFrom.MINECRAFT)
            messageController.sendToTelegram(message)
        }
    }
    val playerLeaveEvent = DSLEvent.event<PlayerQuitEvent> {
        if (!config.displayLeaveMessage) return@event
        PluginScope.launch(Dispatchers.IO) {
            val message = Message.Text(translation.playerLeaveMessage(it.player.name),Message.MessageFrom.MINECRAFT)
            messageController.sendToTelegram(message)
        }
    }
    val messageEvent = DSLEvent.event<ChatEvent> {
        PluginScope.launch(Dispatchers.IO) {
            val textComponent = it.message() as TextComponent
            val message = Message.Text(translation.telegramMessageFormat(it.player.name, textComponent.content()),Message.MessageFrom.MINECRAFT)
            messageController.sendToTelegram(message)
        }
    }
    val deathEvent = DSLEvent.event<PlayerDeathEvent> {
        if (!config.displayDeathMessage) return@event
        PluginScope.launch(Dispatchers.IO) {
            val textComponent = (it.deathMessage() as? TextComponent?)?.content() ?: it.deathMessage
            val message = Message.Text(translation.playerDiedMessage(it.player.name, textComponent ?: "Непанятна че"),Message.MessageFrom.MINECRAFT)
            messageController.sendToTelegram(message)
        }
    }
}

