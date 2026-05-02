package ru.astrainteractive.messagebridge.messenger.forge.event

import kotlinx.coroutines.CoroutineScope
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
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
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
    configKrate: CachedKrate<PluginConfiguration>,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : Logger by JUtiltLogger("MessageBridge-ForgeEvents").withoutParentHandlers() {
    private val config by configKrate

    val serverStartedEvent = flowEvent<ServerStartedEvent>()
        .onEach { info { "#serverStartedEvent" } }
        .onEach {
            scope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
        }.launchIn(scope)

    val serverStoppingEvent = flowEvent<ServerStoppingEvent>()
        .onEach { info { "#serverStoppingEvent" } }
        .onEach {
            scope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
        }.launchIn(scope)

    val playerLoggedOutEvent = flowEvent<PlayerEvent.PlayerLoggedOutEvent>()
        .onEach { info { "#playerLoggedOutEvent" } }
        .filter { config.displayLeaveMessage }
        .onEach {
            scope.launch(dispatchers.IO) {
                val serverEvent = PlayerLeaveBEvent(
                    name = it.entity.name.string,
                    uuid = it.entity.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)

    val playerLoggedInEvent = flowEvent<PlayerEvent.PlayerLoggedInEvent>()
        .onEach { info { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach {
            // doesnt work
//        val nbt = it.entity.persistentData
//        val playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1

            scope.launch(dispatchers.IO) {
                val serverEvent = PlayerJoinedBEvent(
                    name = it.entity.name.string,
                    uuid = it.entity.uuid.toString(),
                    hasPlayedBefore = true
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)

    val livingDeathEvent = flowEvent<LivingDeathEvent>()
        .onEach { info { "#livingDeathEvent" } }
        .filter { config.displayDeathMessage }
        .filter { event -> event.entity is Player }
        .onEach {
            scope.launch(dispatchers.IO) {
                val deathCause = it.source.getLocalizedDeathMessage(it.entity).string
                val serverEvent = PlayerDeathBEvent(
                    name = it.entity.name.string,
                    cause = deathCause,
                    uuid = it.entity.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)

    val serverChatEvent = flowEvent<ServerChatEvent>()
        .onEach { info { "#serverChatEvent" } }
        .onEach {
            scope.launch(dispatchers.IO) {
                val serverEvent = Text.Minecraft(
                    author = it.player.name.string,
                    text = it.message.string,
                    uuid = it.player.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)
}
