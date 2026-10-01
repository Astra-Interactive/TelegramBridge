package ru.astrainteractive.messagebridge.messenger.forge.event

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
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

class NeoForgeEvents(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : Logger by JUtiltLogger("MessageBridge-ForgeEvents") {
    private val config: PluginConfiguration
        get() = configFlow.value

    val serverStartedEvent = flowEvent<ServerStartedEvent>()
        .onEach { verbose { "#serverStartedEvent" } }
        .onEach {
            ioScope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
        }.launchIn(ioScope)

    val serverStoppingEvent = flowEvent<ServerStoppingEvent>()
        .onEach { verbose { "#serverStoppingEvent" } }
        .onEach {
            ioScope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
        }.launchIn(ioScope)

    val playerLoggedOutEvent = flowEvent<PlayerEvent.PlayerLoggedOutEvent>()
        .onEach { verbose { "#playerLoggedOutEvent" } }
        .filter { config.displayLeaveMessage }
        .onEach {
            ioScope.launch(dispatchers.IO) {
                val serverEvent = PlayerLeaveBEvent(
                    name = it.entity.name.string,
                    uuid = it.entity.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val playerLoggedInEvent = flowEvent<PlayerEvent.PlayerLoggedInEvent>()
        .onEach { verbose { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach {
            // doesnt work
//        val nbt = it.entity.persistentData
//        val playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1

            ioScope.launch(dispatchers.IO) {
                val serverEvent = PlayerJoinedBEvent(
                    name = it.entity.name.string,
                    uuid = it.entity.uuid.toString(),
                    hasPlayedBefore = true
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val livingDeathEvent = flowEvent<LivingDeathEvent>()
        .onEach { verbose { "#livingDeathEvent" } }
        .filter { config.displayDeathMessage }
        .filter { event -> event.entity is Player }
        .onEach {
            ioScope.launch(dispatchers.IO) {
                val deathCause = it.source.getLocalizedDeathMessage(it.entity).string
                val serverEvent = PlayerDeathBEvent(
                    name = it.entity.name.string,
                    cause = deathCause,
                    uuid = it.entity.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val serverChatEvent = flowEvent<ServerChatEvent>()
        .onEach { verbose { "#serverChatEvent" } }
        .onEach {
            ioScope.launch(dispatchers.IO) {
                val serverEvent = Text.Minecraft(
                    author = it.player.name.string,
                    text = it.message.string,
                    uuid = it.player.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)
}
