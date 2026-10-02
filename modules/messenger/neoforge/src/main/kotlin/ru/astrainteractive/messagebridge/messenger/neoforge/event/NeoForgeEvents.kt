package ru.astrainteractive.messagebridge.messenger.neoforge.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.event.ServerChatEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import ru.astrainteractive.astralibs.event.flowEvent
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text

internal class NeoForgeEvents(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    private val bEventChannel: BEventChannel
) : Logger by JUtiltLogger("MessageBridge-NeoForgeEvents") {
    private val config: PluginConfiguration
        get() = configFlow.value

    val serverStartedEvent = flowEvent<ServerStartedEvent>()
        .onEach { verbose { "#serverStartedEvent" } }
        .onEach {
            ioScope.launch {
                bEventChannel.consume(ServerOpenBEvent)
            }
        }.launchIn(ioScope)

    val serverStoppingEvent = flowEvent<ServerStoppingEvent>()
        .onEach { verbose { "#serverStoppingEvent" } }
        .onEach {
            ioScope.launch {
                bEventChannel.consume(ServerClosedBEvent)
            }
        }.launchIn(ioScope)

    val playerLoggedOutEvent = flowEvent<PlayerEvent.PlayerLoggedOutEvent>()
        .onEach { verbose { "#playerLoggedOutEvent" } }
        .filter { config.displayLeaveMessage }
        .onEach { event ->
            ioScope.launch(dispatchers.IO) {
                val serverEvent = PlayerLeaveBEvent(
                    name = event.entity.name.string,
                    uuid = event.entity.uuid.toString()
                )
                bEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val playerLoggedInEvent = flowEvent<PlayerEvent.PlayerLoggedInEvent>()
        .onEach { verbose { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach { event ->
            // doesnt work
//        val nbt = event.entity.persistentData
//        val playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1

            ioScope.launch(dispatchers.IO) {
                val serverEvent = PlayerJoinedBEvent(
                    name = event.entity.name.string,
                    uuid = event.entity.uuid.toString(),
                    hasPlayedBefore = true
                )
                bEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val livingDeathEvent = flowEvent<LivingDeathEvent>()
        .onEach { verbose { "#livingDeathEvent" } }
        .filter { config.displayDeathMessage }
        .filter { event -> event.entity is Player }
        .onEach { event ->
            ioScope.launch(dispatchers.IO) {
                val deathCause = event.source.getLocalizedDeathMessage(event.entity).string
                val serverEvent = PlayerDeathBEvent(
                    name = event.entity.name.string,
                    cause = deathCause,
                    uuid = event.entity.uuid.toString()
                )
                bEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val serverChatEvent = flowEvent<ServerChatEvent>()
        .onEach { verbose { "#serverChatEvent" } }
        .onEach { event ->
            ioScope.launch(dispatchers.IO) {
                val serverEvent = Text.Minecraft(
                    author = event.player.name.string,
                    text = event.message.string,
                    uuid = event.player.uuid.toString()
                )
                bEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)
}
