package ru.astrainteractive.messagebridge.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.RequestOnlineMessageData

class SocketEvent(
    private val clientBridgeApi: BridgeApi,
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO),
    Logger by JUtiltLogger("MessageBridge-SocketEvent") {

    private fun onOnlineRequest() {
        info { "#onOnlineRequest requested online players" }
        val data = OnlineListMessageData(
            onlinePlayers = Bukkit.getOnlinePlayers().map(Player::getDisplayName)
        )
        launch { clientBridgeApi.broadcastEvent(data) }
    }

    init {
        clientBridgeApi
            .eventFlow()
            .filterIsInstance<RequestOnlineMessageData>()
            .onEach { onOnlineRequest() }
            .launchIn(this)
    }
}
