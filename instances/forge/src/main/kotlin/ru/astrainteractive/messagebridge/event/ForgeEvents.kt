package ru.astrainteractive.messagebridge.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.minecraft.world.entity.player.Player
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
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

class ForgeEvents(
    configKrate: Krate<PluginConfiguration>,
    private val telegramBEventConsumer: BEventConsumer,
    private val discordBEventConsumer: BEventConsumer,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : ForgeEventBusListener {
    private val config by configKrate

    @Suppress("UnusedParameter")
    @SubscribeEvent
    fun onServerStart(it: ServerStartedEvent) {
        scope.launch {
            telegramBEventConsumer.consume(ServerOpenBEvent)
            discordBEventConsumer.consume(ServerOpenBEvent)
        }
    }

    @Suppress("UnusedParameter")
    @SubscribeEvent
    fun onServerStop(it: ServerStoppingEvent) {
        scope.launch {
            telegramBEventConsumer.consume(ServerClosedBEvent)
            discordBEventConsumer.consume(ServerClosedBEvent)
        }
    }

    @SubscribeEvent
    fun onPlayerLeave(it: PlayerLoggedOutEvent) {
        if (!config.displayLeaveMessage) return
        scope.launch(dispatchers.IO) {
            val serverEvent = PlayerLeaveBEvent(
                name = it.entity.name.string,
                uuid = it.entity.uuid.toString()
            )
            telegramBEventConsumer.consume(serverEvent)
            discordBEventConsumer.consume(serverEvent)
        }
    }

    @SubscribeEvent
    fun onPlayerJoin(it: PlayerLoggedInEvent) {
        if (!config.displayJoinMessage) return
        // doesnt work
//        val nbt = it.entity.persistentData
//        val playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1

        scope.launch(dispatchers.IO) {
            val serverEvent = PlayerJoinedBEvent(
                name = it.entity.name.string,
                uuid = it.entity.uuid.toString(),
                hasPlayedBefore = true
            )
            telegramBEventConsumer.consume(serverEvent)
            discordBEventConsumer.consume(serverEvent)
        }
    }

    @SubscribeEvent
    fun onPlayerDeath(it: LivingDeathEvent) {
        if (!config.displayDeathMessage) return
        if (it.entity !is Player) return
        scope.launch(dispatchers.IO) {
            val deathCause = it.source.getLocalizedDeathMessage(it.entity).string
            val serverEvent = PlayerDeathBEvent(
                name = it.entity.name.string,
                cause = deathCause,
                uuid = it.entity.uuid.toString()
            )
            telegramBEventConsumer.consume(serverEvent)
            discordBEventConsumer.consume(serverEvent)
        }
    }

    @SubscribeEvent
    fun onChat(it: ServerChatEvent) {
        scope.launch(dispatchers.IO) {
            val serverEvent = Text.Minecraft(
                author = it.player.name.string,
                text = it.message.string,
                uuid = it.player.uuid.toString()
            )
            telegramBEventConsumer.consume(serverEvent)
            discordBEventConsumer.consume(serverEvent)
        }
    }
}
