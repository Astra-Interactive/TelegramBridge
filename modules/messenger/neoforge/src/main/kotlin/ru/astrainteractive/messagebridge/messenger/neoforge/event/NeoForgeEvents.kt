package ru.astrainteractive.messagebridge.messenger.neoforge.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.event.ServerChatEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import ru.astrainteractive.astralibs.event.flowEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text

class NeoForgeEvents(
    configKrate: CachedKrate<PluginConfiguration>,
    private val ioScope: CoroutineScope,
    private val bEventConsumer: BEventConsumer
) : Logger by JUtiltLogger("MessageBridge-NeoForgeEvents") {
    private val config by configKrate

    val playerLoggedOutEvent = flowEvent<PlayerEvent.PlayerLoggedOutEvent>()
        .onEach { verbose { "#playerLoggedOutEvent" } }
        .filter { config.displayLeaveMessage }
        .onEach { event ->
            val serverEvent = PlayerLeaveBEvent(
                name = event.entity.name.string,
                uuid = event.entity.uuid.toString()
            )
            bEventConsumer.consume(serverEvent)
        }.launchIn(ioScope)

    val playerLoggedInEvent = flowEvent<PlayerEvent.PlayerLoggedInEvent>()
        .onEach { verbose { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach { event ->
            // doesnt work
//        val nbt = event.entity.persistentData
//        val playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1

            val serverEvent = PlayerJoinedBEvent(
                name = event.entity.name.string,
                uuid = event.entity.uuid.toString(),
                hasPlayedBefore = true
            )
            bEventConsumer.consume(serverEvent)
        }.launchIn(ioScope)

    val livingDeathEvent = flowEvent<LivingDeathEvent>()
        .onEach { verbose { "#livingDeathEvent" } }
        .filter { config.displayDeathMessage }
        .filter { event -> event.entity is Player }
        .onEach { event ->
            val deathCause = event.source.getLocalizedDeathMessage(event.entity).string
            val serverEvent = PlayerDeathBEvent(
                name = event.entity.name.string,
                cause = deathCause,
                uuid = event.entity.uuid.toString()
            )
            bEventConsumer.consume(serverEvent)
        }.launchIn(ioScope)

    val serverChatEvent = flowEvent<ServerChatEvent>()
        .onEach { verbose { "#serverChatEvent" } }
        .onEach { event ->
            val serverEvent = Text.Minecraft(
                author = event.player.name.string,
                text = event.message.string,
                uuid = event.player.uuid.toString()
            )
            bEventConsumer.consume(serverEvent)
        }.launchIn(ioScope)
}
