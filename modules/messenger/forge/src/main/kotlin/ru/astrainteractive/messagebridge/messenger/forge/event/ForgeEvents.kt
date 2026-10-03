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
import net.minecraftforge.eventbus.api.EventPriority
import ru.astrainteractive.astralibs.event.flowEvent
import ru.astrainteractive.astralibs.server.util.toPlain
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

class ForgeEvents(
    configKrate: CachedKrate<PluginConfiguration>,
    private val ioScope: CoroutineScope,
    private val dispatchers: KotlinDispatchers,
    private val bEventConsumer: BEventConsumer
) : Logger by JUtiltLogger("MessageBridge-ForgeEvents").withoutParentHandlers() {
    private val config by configKrate

    val playerLoggedOutEvent = flowEvent<PlayerEvent.PlayerLoggedOutEvent>()
        .onEach { verbose { "#playerLoggedOutEvent" } }
        .filter { config.displayLeaveMessage }
        .onEach { event ->
            ioScope.launch(dispatchers.IO) {
                val serverEvent = PlayerLeaveBEvent(
                    name = event.entity.name.string,
                    uuid = event.entity.uuid.toString()
                )
                bEventConsumer.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val playerLoggedInEvent = flowEvent<PlayerEvent.PlayerLoggedInEvent>()
        .onEach { verbose { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach { event ->
            ioScope.launch(dispatchers.IO) {
                val serverEvent = PlayerJoinedBEvent(
                    name = event.entity.name.string,
                    uuid = event.entity.uuid.toString(),
                    hasPlayedBefore = true
                )
                bEventConsumer.consume(serverEvent)
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
                bEventConsumer.consume(serverEvent)
            }
        }.launchIn(ioScope)

    val serverChatEvent = flowEvent<ServerChatEvent>(EventPriority.HIGHEST)
        .onEach { verbose { "#serverChatEvent" } }
        .onEach { event ->
            ioScope.launch(dispatchers.IO) {
                val serverEvent = Text.Minecraft(
                    author = event.player.name.string,
                    text = event.message.toPlain(),
                    uuid = event.player.uuid.toString()
                )
                bEventConsumer.consume(serverEvent)
            }
        }.launchIn(ioScope)
}
