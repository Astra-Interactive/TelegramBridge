package ru.astrainteractive.messagebridge.messenger.forge.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import ru.astrainteractive.astralibs.event.flowEvent
import ru.astrainteractive.astralibs.server.util.toPlain
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

class ForgeEvents(
    configKrate: CachedKrate<PluginConfiguration>,
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : Logger by JUtiltLogger("MessageBridge-ForgeEvents").withoutParentHandlers() {
    private val config by configKrate

    val serverStartedEvent = flowEvent<ServerStartedEvent>()
        .onEach { info { "#serverStartedEvent" } }
        .onEach {
            ioScope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
        }.launchIn(ioScope)

    val serverStoppingEvent = flowEvent<ServerStoppingEvent>()
        .onEach { info { "#serverStoppingEvent" } }
        .onEach {
            ioScope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
        }.launchIn(ioScope)

    val playerLoggedOutEvent = flowEvent<PlayerEvent.PlayerLoggedOutEvent>()
        .onEach { info { "#playerLoggedOutEvent" } }
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
        .onEach { info { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach {
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
        .onEach { info { "#livingDeathEvent" } }
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
        .onEach { info { "#serverChatEvent" } }
        .onEach {
            ioScope.launch(dispatchers.IO) {
                val serverEvent = Text.Minecraft(
                    author = it.player.name.string,
                    text = it.message.toPlain(),
                    uuid = it.player.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(ioScope)
}
