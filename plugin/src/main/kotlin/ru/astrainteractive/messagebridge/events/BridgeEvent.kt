package ru.astrainteractive.messagebridge.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.PluginBridgeApi

/**
 * Events from discord by socket
 */
class BridgeEvent(
    clientBridgeApi: BridgeApi,
    private val pluginBridgeApi: PluginBridgeApi,
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO) {

    init {
        clientBridgeApi
            .eventFlow()
            .onEach { pluginBridgeApi.broadcastEvent(it) }
            .launchIn(this)
    }
}
