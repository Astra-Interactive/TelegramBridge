package ru.astrainteractive.messagebridge.messenger.forge.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.forge.core.event.flowEvent
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

class ForgeEvents(
    configKrate: Krate<PluginConfiguration>,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) {
    private val config by configKrate

    val serverStartedEvent = flowEvent<ServerStartedEvent>()
        .onEach {
            scope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
        }.launchIn(scope)

    val serverStoppingEvent = flowEvent<ServerStoppingEvent>()
        .onEach {
            scope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
        }.launchIn(scope)

    val playerLoggedOutEvent = flowEvent<PlayerLoggedOutEvent>()
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

    val playerLoggedInEvent = flowEvent<PlayerLoggedInEvent>()
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
