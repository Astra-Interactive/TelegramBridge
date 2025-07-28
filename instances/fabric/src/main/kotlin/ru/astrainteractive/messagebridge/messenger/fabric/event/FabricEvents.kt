package ru.astrainteractive.messagebridge.messenger.fabric.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.util.getValue
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.util.fabricEventFlow
import ru.astrainteractive.messagebridge.core.util.send
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messaging.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text

class FabricEvents(
    configKrate: CachedKrate<PluginConfiguration>,
    private val scope: CoroutineScope,
    private val dispatchers: KotlinDispatchers
) : Logger by JUtiltLogger("MessageBridge-ForgeEvents") {
    private val config by configKrate

    val serverStartedEvent = fabricEventFlow {
        val callback = ServerTickEvents.StartTick(::send)
        ServerTickEvents.START_SERVER_TICK.register(callback)
    }
        .onEach { info { "#serverStartedEvent" } }
        .onEach {
            scope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
        }.launchIn(scope)

    val serverStoppingEvent = fabricEventFlow {
        val callback = ServerTickEvents.EndTick(::send)
        ServerTickEvents.END_SERVER_TICK.register(callback)
    }
        .onEach { info { "#serverStoppingEvent" } }
        .onEach {
            scope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
        }.launchIn(scope)

    val playerLoggedOutEvent = fabricEventFlow {
        val callback = ServerPlayConnectionEvents.Disconnect(::send)
        ServerPlayConnectionEvents.DISCONNECT.register(callback)
    }
        .onEach { info { "#playerLoggedOutEvent" } }
        .filter { config.displayLeaveMessage }
        .onEach { (handler, server) ->
            val player = handler.player
            scope.launch(dispatchers.IO) {
                val serverEvent = PlayerLeaveBEvent(
                    name = player.name.string,
                    uuid = player.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)

    val playerLoggedInEvent = fabricEventFlow {
        val callback = ServerPlayConnectionEvents.Join(::send)
        ServerPlayConnectionEvents.JOIN.register(callback)
    }
        .onEach { info { "#playerLoggedInEvent" } }
        .filter { config.displayJoinMessage }
        .onEach { (handler, sender, server) ->
            val player = handler.player
            scope.launch(dispatchers.IO) {
                val serverEvent = PlayerJoinedBEvent(
                    name = player.name.string,
                    uuid = player.uuid.toString(),
                    hasPlayedBefore = true
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)

    val livingDeathEvent = fabricEventFlow {
        val callback = ServerEntityCombatEvents.AfterKilledOtherEntity(::send)
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(callback)
    }
        .onEach { info { "#livingDeathEvent" } }
        .filter { config.displayDeathMessage }
        .filter { (_, _, killedEntity) -> killedEntity.isPlayer }
        .onEach { (_, _, killedEntity) ->
            scope.launch(dispatchers.IO) {
                val deathCause = null
                val serverEvent = PlayerDeathBEvent(
                    name = killedEntity.name.string,
                    cause = deathCause,
                    uuid = killedEntity.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)

    val serverChatEvent = fabricEventFlow {
        val callback = ServerMessageEvents.ChatMessage(::send)
        ServerMessageEvents.CHAT_MESSAGE.register(callback)
    }
        .onEach { info { "#serverChatEvent" } }
        .onEach { (signedMessage, player, params) ->
            scope.launch(dispatchers.IO) {
                val serverEvent = Text.Minecraft(
                    author = player.name.string,
                    text = signedMessage.content.string,
                    uuid = player.uuid.toString()
                )
                BEventChannel.consume(serverEvent)
            }
        }.launchIn(scope)
}
