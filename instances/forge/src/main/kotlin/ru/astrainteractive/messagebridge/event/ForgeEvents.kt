package ru.astrainteractive.messagebridge.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.event.core.ForgeEventBusListener
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class ForgeEvents(
    configKrate: Krate<PluginConfiguration>,
    private val telegramMessageController: MessageController,
    private val discordMessageController: MessageController,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : ForgeEventBusListener {
    private val config by configKrate

    @Suppress("UnusedParameter")
    @SubscribeEvent
    fun onServerStart(it: ServerStartedEvent) {
        scope.launch {
            telegramMessageController.send(ServerEvent.ServerOpen)
            discordMessageController.send(ServerEvent.ServerOpen)
        }
    }

    @Suppress("UnusedParameter")
    @SubscribeEvent
    fun onServerStop(it: ServerStoppingEvent) {
        scope.launch {
            telegramMessageController.send(ServerEvent.ServerClosed)
            discordMessageController.send(ServerEvent.ServerClosed)
        }
    }

    @SubscribeEvent
    fun onPlayerLeave(it: PlayerLoggedOutEvent) {
        if (!config.displayLeaveMessage) return
        scope.launch(dispatchers.IO) {
            val serverEvent = ServerEvent.PlayerLeave(
                name = it.entity.name.string,
                uuid = it.entity.uuid.toString()
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }

    @SubscribeEvent
    fun onPlayerJoin(it: PlayerLoggedInEvent) {
        if (!config.displayJoinMessage) return
        // doesnt work
//        val nbt = it.entity.persistentData
//        val playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1

        scope.launch(dispatchers.IO) {
            val serverEvent = ServerEvent.PlayerJoined(
                name = it.entity.name.string,
                uuid = it.entity.uuid.toString(),
                hasPlayedBefore = true
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }

    @SubscribeEvent
    fun onPlayerDeath(it: LivingDeathEvent) {
        if (!config.displayDeathMessage) return

        scope.launch(dispatchers.IO) {
            val deathCause = it.source.getLocalizedDeathMessage(it.entity).string
            val serverEvent = ServerEvent.PlayerDeath(
                name = it.entity.name.string,
                cause = deathCause,
                uuid = it.entity.uuid.toString()
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }

    @SubscribeEvent
    fun onChat(it: ServerChatEvent) {
        scope.launch(dispatchers.IO) {
            val serverEvent = ServerEvent.Text.Minecraft(
                author = it.player.name.string,
                text = it.message.string,
                uuid = it.player.uuid.toString()
            )
            telegramMessageController.send(serverEvent)
            discordMessageController.send(serverEvent)
        }
    }
}
