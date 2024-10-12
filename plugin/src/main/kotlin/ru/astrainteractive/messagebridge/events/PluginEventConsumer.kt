package ru.astrainteractive.messagebridge.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.PluginBridgeApi
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messaging.MinecraftMessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class PluginEventConsumer(
    private val pluginBridgeApi: PluginBridgeApi,
    private val telegramMessageController: MessageController,
    private val minecraftMessageController: MinecraftMessageController,
    private val clientBridgeApi: BridgeApi,
) : CoroutineFeature by CoroutineFeature.Default(Dispatchers.IO) {

    private suspend fun onEvent(event: ServerEvent) {
        if (event.from != ServerEvent.MessageFrom.DISCORD) {
            clientBridgeApi.broadcastEvent(event)
        }
        if (event.from != ServerEvent.MessageFrom.TELEGRAM) {
            telegramMessageController.send(event)
        }
        if (event.from != ServerEvent.MessageFrom.MINECRAFT) {
            minecraftMessageController.send(event)
        }
    }

    init {
        pluginBridgeApi.eventFlow()
            .onEach { onEvent(it) }
            .launchIn(this)
    }
}
